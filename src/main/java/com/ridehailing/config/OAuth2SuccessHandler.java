package com.ridehailing.config;

import com.ridehailing.model.Role;
import com.ridehailing.model.Users;
import com.ridehailing.repository.UserRepository;
import com.ridehailing.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Slf4j
@Component
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final JwtService service;

    public OAuth2SuccessHandler(UserRepository userRepository, JwtService service){
        this.userRepository = userRepository;
        this.service = service;
    }

    @Value("${frontend.url:http://localhost:5173}")
    private String frontendBaseUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        FilterChain chain, Authentication authentication)
            throws IOException, ServletException {
        AuthenticationSuccessHandler.super.onAuthenticationSuccess(request, response, chain, authentication);
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        Boolean emailVerified = oAuth2User.getAttribute("email_verified");

        if (email == null || email.isBlank() || Boolean.FALSE.equals(emailVerified)) {
            log.warn("OAuth2: Rejected unverified or missing Google email");
            response.sendRedirect(frontendBaseUrl + "/login?error=unverified_email");
            return;
        }

        String normalizedEmail = email.trim().toLowerCase();
        Optional<Users> existingUser = userRepository.findByEmail(normalizedEmail);
        boolean isNew = existingUser.isEmpty();
        Users user;

        if(isNew){
            user = new Users();
            user.setEmail(normalizedEmail);
            user.setName(name != null && !name.isBlank() ? name.trim() : null);
            user.setPassword("");
            user.setRole(Role.ROLE_RIDER);
            user.setProvider("GOOGLE");
            user = userRepository.save(user);
            log.info("OAuth2: New user registered via Google SSO: {}", normalizedEmail);
        }
        else{
            user = existingUser.get();
            boolean updated = false;

            if (!"GOOGLE".equalsIgnoreCase(user.getProvider())) {
                user.setProvider("GOOGLE");
                user.setPassword("");
                updated = true;
                log.info("OAuth2: Upgraded existing LOCAL account to verified GOOGLE provider: {}", normalizedEmail);
            }

            if ((user.getName() == null || user.getName().isBlank()) && name != null && !name.isBlank()) {
                user.setName(name.trim());
                updated = true;
            }

            if (updated) {
                user = userRepository.save(user);
            }
            log.info("OAuth2: User {} logged in via Google SSO (account provider: {})", normalizedEmail, user.getProvider());
        }

        String roleName = user.getRole() != null ? user.getRole().name() : Role.ROLE_RIDER.name();
        String token = service.generateToken(user.getEmail(), roleName);

        String encodedName = name != null ? java.net.URLEncoder.encode(name, StandardCharsets.UTF_8) : "";

        String frontEndUrl = frontendBaseUrl + "/oauth2/redirect#token=" + token
                + (!encodedName.isEmpty() ? "&name=" + encodedName : "")
                + "&isNew=" + isNew;

        response.sendRedirect(frontEndUrl);
    }
}