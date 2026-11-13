package com.ridehailing.service;

import com.ridehailing.dto.ProfileCompletionRequest;
import com.ridehailing.dto.UserRegistrationRequest;
import com.ridehailing.exception.ForbiddenException;
import com.ridehailing.exception.ResourceNotFoundException;
import com.ridehailing.exception.RideConflictException;
import com.ridehailing.model.*;
import com.ridehailing.repository.DriverRepository;
import com.ridehailing.repository.RiderRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.ridehailing.repository.UserRepository;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RiderRepository riderRepository;
    private final DriverRepository driverRepository;

    public UserService(PasswordEncoder passwordEncoder,
                       UserRepository userRepository,
                       AuthenticationManager authenticationManager,
                       JwtService jwtService,
                       RiderRepository riderRepository,
                       DriverRepository driverRepository){
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.riderRepository = riderRepository;
        this.driverRepository = driverRepository;
    }

    public Users register(UserRegistrationRequest request) {

        String email = request.getEmail().trim().toLowerCase();

        if(userRepository.findByEmail(email).isPresent()){
            throw new RideConflictException("Error: Email is already registered!");
        }

        Users newUser = new Users();
        newUser.setEmail(email);
        newUser.setPassword(passwordEncoder.encode(request.getPassword()));
        newUser.setRole(Role.ROLE_RIDER);
        if (request.getName() != null && !request.getName().trim().isEmpty()) {
            newUser.setName(request.getName().trim());
        }

        return userRepository.save(newUser);
    }

    public String verify(Users user) {

        String email = user.getEmail().trim().toLowerCase();

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, user.getPassword())
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
            return jwtService.generateToken(email, roleName);
        }
        return "Fail";
    }

    @Transactional
    public String completeUserProfile(String email, ProfileCompletionRequest request){

        Users user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Error: User not found in database!"));

        String cleanName = (request.getName() != null && !request.getName().trim().isEmpty())
                ? request.getName().trim()
                : null;
        String cleanPhone = (request.getPhone() != null && !request.getPhone().trim().isEmpty())
                ? request.getPhone().trim()
                : null;

        boolean isAlreadyRider = riderRepository.findByUsers_Email(email).isPresent();
        boolean isAlreadyDriver = driverRepository.findByUser_Email(email).isPresent();

        if (isAlreadyRider) {
            Riders rider = riderRepository.findByUsers_Email(email).get();
            if (cleanName != null) {
                rider.setRiderName(cleanName);
                user.setName(cleanName);
                userRepository.save(user);
            }
            if (cleanPhone != null) {
                rider.setPhoneNum(cleanPhone);
            }
            riderRepository.save(rider);
            return "Profile updated successfully!";
        } else if (isAlreadyDriver) {
            Driver driver = driverRepository.findByUser_Email(email).get();
            if (cleanName != null) {
                driver.setDriverName(cleanName);
                user.setName(cleanName);
                userRepository.save(user);
            }
            driverRepository.save(driver);
            return "Profile updated successfully!";
        }

        // Dedicated profile update handling for Administrators
        if (user.getRole() == Role.ROLE_ADMIN || "ROLE_ADMIN".equalsIgnoreCase(request.getRole())) {
            if (cleanName != null) {
                user.setName(cleanName);
            }
            userRepository.save(user);
            return "Admin profile updated successfully!";
        }

        String requestedRole = (request.getRole() != null) ? request.getRole().toUpperCase().trim() : "ROLE_RIDER";

        if(!requestedRole.equals("ROLE_RIDER") && !requestedRole.equals("ROLE_DRIVER")){
            throw new ForbiddenException("Error: Unauthorized or invalid role selection!");
        }

        user.setRole(Role.valueOf(requestedRole));
        if (cleanName != null) {
            user.setName(cleanName);
        }
        userRepository.save(user);

        if(requestedRole.equalsIgnoreCase("ROLE_RIDER")){
            Riders rider = riderRepository.findByUsers_Email(email).orElse(new Riders());
            rider.setUsers(user);
            rider.setRiderName(cleanName != null ? cleanName : "SafarX Rider");
            rider.setPhoneNum(cleanPhone != null ? cleanPhone : "");
            if (rider.getRiderRating() == null || rider.getRiderRating() == 0.0) {
                rider.setRiderRating(5.0);
            }
            riderRepository.save(rider);
            return "Rider profile created Successfully";
        }
        else if ("ROLE_DRIVER".equalsIgnoreCase(requestedRole)) {
            Driver driver = driverRepository.findByUser_Email(email).orElse(new Driver());
            driver.setUser(user);
            driver.setDriverName(cleanName != null ? cleanName : "SafarX Driver");
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
