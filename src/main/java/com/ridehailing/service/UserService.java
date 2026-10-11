package com.ridehailing.service;

import com.ridehailing.dto.ProfileCompletionRequest;
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

    public Users register(Users user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        if(user.getRole() == null){
            user.setRole(Role.ROLE_RIDER);
        }

        return userRepository.save(user);
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
                .orElseThrow(() -> new RuntimeException("Error: User not found in database!"));

        user.setRole(Role.valueOf(request.getRole()));
        userRepository.save(user);

        if(request.getRole().equalsIgnoreCase("ROLE_RIDER")){
            Riders rider = new Riders();
            rider.setUsers(user);
            rider.setRiderName(request.getName());
            rider.setPhoneNum(request.getPhone());
            rider.setRiderRating(5.0);
            riderRepository.save(rider);
            return "Rider profile created Successfully";
        }
        else if (request.getRole().equalsIgnoreCase("ROLE_DRIVER")) {
            Driver driver = new Driver();
            driver.setUser(user);
            driver.setDriverName(request.getName());
            driver.setRating(5.0);
            driver.setDriverStatus(VehicleStatus.OFFLINE);
            driverRepository.save(driver);
            return "Driver profile created successfully! Please register a vehicle next.";
        }

        throw new RuntimeException("Error: Invalid Role provided!");
    }
}
