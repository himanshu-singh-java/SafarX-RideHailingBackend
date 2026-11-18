package com.ridehailing.config;

import com.ridehailing.model.Role;
import com.ridehailing.model.Users;
import com.ridehailing.repository.UserRepository;
import com.ridehailing.service.JwtService;
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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Optional;

@Slf4j
@Component
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final JwtService service;
    private final String frontendBaseUrl;


    public OAuth2SuccessHandler(UserRepository userRepository,
                                JwtService service,
                                @Value("${frontend.url:http://localhost:5173}") String frontendBaseUrl){
        this.userRepository = userRepository;
        this.service = service;
        this.frontendBaseUrl = frontendBaseUrl.replaceAll("/+$", "");
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        response.setHeader("Cache-Control", "no-store");
        response.setHeader("Pragma", "no-cache");

        if (!(authentication.getPrincipal() instanceof OAuth2User oAuth2User)) {
            log.warn("OAuth2: Unexpected principal type during authentication");
            response.sendRedirect(frontendBaseUrl + "/login?error=oauth_failed");
            return;
        }

        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        Object emailVerifiedAttr = oAuth2User.getAttribute("email_verified");


        Boolean isEmailVerified = Boolean.TRUE.equals(emailVerifiedAttr) ||
                "true".equalsIgnoreCase(String.valueOf(emailVerifiedAttr));

        if (email == null || email.isBlank() || !isEmailVerified) {
            log.warn("OAuth2: Rejected unverified or missing Google email");
            response.sendRedirect(frontendBaseUrl + "/login?error=unverified_email");
            return;
        }

        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        String sanitizedName = (name != null && !name.isBlank()) ? name.trim() : null;

        Optional<Users> existingUser = userRepository.findByEmail(normalizedEmail);
        boolean isNew = existingUser.isEmpty();
        Users user;

        if(isNew){
            user = new Users();
            user.setEmail(normalizedEmail);
            user.setName(sanitizedName);
            user.setPassword("");
            user.setRole(Role.ROLE_RIDER);
            user.setProvider("GOOGLE");
            user = userRepository.save(user);
            log.info("OAuth2: New user registered via Google SSO: {}", normalizedEmail);
        }
        else{
            user = existingUser.get();

            if ((user.getName() == null || user.getName().isBlank()) && sanitizedName != null) {
                user.setName(sanitizedName);
                user = userRepository.save(user);
            }
            log.info("OAuth2: User {} logged in via Google SSO (account provider: {})", normalizedEmail, user.getProvider());
        }

        String roleName = (user.getRole() != null) ? user.getRole().name() : Role.ROLE_RIDER.name();
        String token = service.generateToken(user.getEmail(), roleName);

        String displayName = (user.getName() != null && !user.getName().isBlank())
                ? user.getName()
                : (sanitizedName != null ? sanitizedName : "");

        String encodedName = !displayName.isEmpty() ? URLEncoder.encode(displayName,
                StandardCharsets.UTF_8).replace("+", "%20") : "";

        String redirectUrl = frontendBaseUrl + "/oauth2/redirect#token=" + token
                + (!encodedName.isEmpty() ? "&name=" + encodedName : "")
                + "&isNew=" + isNew;

        response.sendRedirect(redirectUrl);
    }
}