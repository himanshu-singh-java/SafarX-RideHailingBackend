package com.ridehailing.service;

import com.ridehailing.dto.ProfileCompletionRequest;
import com.ridehailing.dto.UserRegistrationRequest;
import com.ridehailing.exception.ForbiddenException;
import com.ridehailing.exception.ResourceNotFoundException;
import com.ridehailing.exception.RideConflictException;
import com.ridehailing.model.*;
import com.ridehailing.repository.DriverRepository;
import com.ridehailing.repository.RiderRepository;
import com.ridehailing.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

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

        if (request.getRole() != null) {
            String requestedRole = request.getRole().trim().toUpperCase();
            if ("ROLE_ADMIN".equalsIgnoreCase(requestedRole) || "ADMIN".equalsIgnoreCase(requestedRole)) {
                throw new ForbiddenException("Cannot self-assign Admin role");
            }
        }

        if(userRepository.findByEmail(email).isPresent()){
            throw new RideConflictException("Error: Email is already registered!");
        }

        Users newUser = new Users();
        newUser.setEmail(email);
        newUser.setPassword(passwordEncoder.encode(request.getPassword()));
        newUser.setRole(Role.ROLE_RIDER);
        newUser.setProvider("LOCAL");
        if (request.getName() != null && !request.getName().trim().isEmpty()) {
            newUser.setName(request.getName().trim());
        }

        return userRepository.save(newUser);
    }

    public String verify(Users user) {
        String email = user.getEmail().trim().toLowerCase();

        Optional<Users> existingUserOpt = userRepository.findByEmail(email);
        if (existingUserOpt.isPresent() && "GOOGLE".equalsIgnoreCase(existingUserOpt.get().getProvider())) {
            throw new ForbiddenException("This account is linked with Google. Please use 'Continue with Google' to sign in.");
        }

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, user.getPassword())
        );

        if(authentication.isAuthenticated()){
            String roleName = Role.ROLE_RIDER.name();
            Object principal = authentication.getPrincipal();
            if (principal instanceof UserPrincipal userPrincipal) {
                Users dbUser = userPrincipal.getUser();
                if (dbUser != null && dbUser.getRole() != null) {
                    roleName = dbUser.getRole().name();
                }
            }
            return jwtService.generateToken(email, roleName);
        }
        throw new ForbiddenException("Invalid email or password");
    }

    private Role resolveTargetRole(String rawRequestedRole) {
        if (rawRequestedRole == null || rawRequestedRole.isBlank()) {
            return null;
        }
        String normalized = rawRequestedRole.trim().toUpperCase();
        if ("ROLE_ADMIN".equals(normalized) || "ADMIN".equals(normalized)) {
            throw new ForbiddenException("Cannot self-assign Admin role");
        }
        if ("ROLE_RIDER".equals(normalized) || "RIDER".equals(normalized)) {
            return Role.ROLE_RIDER;
        }
        if ("ROLE_DRIVER".equals(normalized) || "DRIVER".equals(normalized)) {
            return Role.ROLE_DRIVER;
        }
        throw new ForbiddenException("Error: Unauthorized or invalid role selection!");
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

        if (user.getRole() == Role.ROLE_ADMIN) {
            if (cleanName != null) {
                user.setName(cleanName);
            }
            userRepository.save(user);
            return "Admin profile updated successfully!";
        }

        Role requestedRole = resolveTargetRole(request.getRole());

        Optional<Riders> existingRiderOpt = riderRepository.findByUsers_Email(email);
        Optional<Driver> existingDriverOpt = driverRepository.findByUser_Email(email);

        if (existingRiderOpt.isPresent() && requestedRole == Role.ROLE_DRIVER) {
            Riders existingRider = existingRiderOpt.get();
            if (existingRider.getPhoneNum() == null || existingRider.getPhoneNum().isBlank()) {
                riderRepository.delete(existingRider);
                riderRepository.flush();
                existingRiderOpt = Optional.empty();
            }
        }

        if (existingRiderOpt.isPresent()) {
            Riders rider = existingRiderOpt.get();
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
        }

        if (existingDriverOpt.isPresent()) {
            Driver driver = existingDriverOpt.get();
            if (cleanName != null) {
                driver.setDriverName(cleanName);
                user.setName(cleanName);
                userRepository.save(user);
            }
            if (cleanPhone != null) {
                driver.setPhone(cleanPhone);
            }
            driverRepository.save(driver);
            return "Profile updated successfully!";
        }

        Role finalRole = (requestedRole != null) ? requestedRole : Role.ROLE_RIDER;
        String finalName = cleanName != null ? cleanName : user.getName();
        if (finalName == null || finalName.isBlank()) {
            throw new IllegalArgumentException("Full name is required to complete profile");
        }

        user.setRole(finalRole);
        user.setName(finalName);
        userRepository.save(user);

        if (finalRole == Role.ROLE_RIDER) {
            Riders rider = new Riders();
            rider.setUsers(user);
            rider.setRiderName(finalName);
            rider.setPhoneNum(cleanPhone != null ? cleanPhone : "");
            rider.setRiderRating(5.0);
            riderRepository.save(rider);
            return "Rider profile created Successfully";
        } else {
            Driver driver = new Driver();
            driver.setUser(user);
            driver.setDriverName(finalName);
            driver.setPhone(cleanPhone);
            driver.setRating(5.0);
            driver.setDriverStatus(VehicleStatus.OFFLINE);
            driverRepository.save(driver);
            return "Driver profile created successfully! Please register a vehicle next.";
        }
    }
}