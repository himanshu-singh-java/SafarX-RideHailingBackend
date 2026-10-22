package com.ridehailing.service;

import com.ridehailing.dto.ProfileCompletionRequest;
import com.ridehailing.dto.UserRegistrationRequest;
import com.ridehailing.exception.ResourceNotFoundException;
import com.ridehailing.model.*;
import com.ridehailing.repository.DriverRepository;
import com.ridehailing.repository.RiderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.ridehailing.repository.UserRepository;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private RiderRepository riderRepository;

    @Autowired
    private DriverRepository driverRepository;

    public Users register(UserRegistrationRequest request) {

        Users newUser = new Users();
        newUser.setEmail(request.getEmail());
        newUser.setPassword(passwordEncoder.encode(request.getPassword()));

        newUser.setRole(Role.ROLE_RIDER);

        return userRepository.save(newUser);
    }

    public String verify(Users user) {

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(user.getEmail(), user.getPassword())
        );

        if(authentication.isAuthenticated()){
            String roleName = "ROLE_RIDER";
            Object principal = authentication.getPrincipal();
            if (principal instanceof com.ridehailing.model.UserPrincipal) {
                com.ridehailing.model.Users dbUser = ((com.ridehailing.model.UserPrincipal) principal).getUser();
                if (dbUser != null && dbUser.getRole() != null) {
                    roleName = dbUser.getRole().name();
                }
            }
            return jwtService.generateToken(user.getEmail(), roleName);
        }
        return "Fail";
    }

    @Transactional
    public String completeUserProfile(String email, ProfileCompletionRequest request){

        Users user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Error: User not found in database!"));

        user.setRole(Role.valueOf(request.getRole()));
        userRepository.save(user);

        if(request.getRole().equalsIgnoreCase("ROLE_RIDER")){
            Riders rider = riderRepository.findByUsers_Email(email).orElse(new Riders());
            rider.setUsers(user);
            rider.setRiderName(request.getName());
            rider.setPhoneNum(request.getPhone());
            if (rider.getRiderRating() == null || rider.getRiderRating() == 0.0) {
                rider.setRiderRating(5.0);
            }
            riderRepository.save(rider);
            return "Rider profile created Successfully";
        }
        else if (request.getRole().equalsIgnoreCase("ROLE_DRIVER")) {
            Driver driver = driverRepository.findByUser_Email(email).orElse(new Driver());
            driver.setUser(user);
            driver.setDriverName(request.getName());
            if (driver.getRating() == 0.0) {
                driver.setRating(5.0);
            }
            if (driver.getDriverStatus() == null) {
                driver.setDriverStatus(VehicleStatus.OFFLINE);
            }
            driverRepository.save(driver);
            return "Driver profile created successfully! Please register a vehicle next.";
        }

        throw new RuntimeException("Error: Invalid Role provided!");
    }
}
