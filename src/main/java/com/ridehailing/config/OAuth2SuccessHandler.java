package com.ridehailing.config;

import com.ridehailing.model.Riders;
import com.ridehailing.model.Role;
import com.ridehailing.model.Users;
import com.ridehailing.repository.RiderRepository;
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
import java.util.Optional;

@Slf4j
@Component
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final RiderRepository riderRepository;
    private final JwtService service;

    public OAuth2SuccessHandler(UserRepository userRepository,
                                RiderRepository riderRepository,
                                JwtService service){
        this.userRepository = userRepository;
        this.riderRepository = riderRepository;
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

        Optional<Users> existingUser = userRepository.findByEmail(email);
        boolean isNew = existingUser.isEmpty();
        Users user;

        if(isNew){
            user = new Users();
            user.setEmail(email);
            user.setName(name != null && !name.isBlank() ? name.trim() : null);
            user.setPassword("");
            user.setRole(Role.ROLE_RIDER);
            user.setProvider("GOOGLE");
            user = userRepository.save(user);
            log.info("OAuth2: New user registered via Google SSO: {}", email);
        }
        else{
            user = existingUser.get();
            if (!"GOOGLE".equals(user.getProvider())) {
                log.warn("OAuth: blocked Google login for non-Google account: {}", email);
                response.sendRedirect(frontendBaseUrl + "/login?error=account_exists_use_password");
                return;
            }
            if ((user.getName() == null || user.getName().isBlank()) && name != null && !name.isBlank()) {
                user.setName(name.trim());
                userRepository.save(user);
            }
            log.info("OAuth2: User {} logged in via Google SSO (account provider: {})", email, user.getProvider());
        }

        if(user.getRole() == Role.ROLE_RIDER && riderRepository.findByUsers_Email(email).isEmpty()){
            Riders rider = new Riders();
            rider.setUsers(user);
            rider.setRiderName(name != null && !name.isBlank() ? name : "SafarX Rider");
            rider.setPhoneNum("");
            rider.setRiderRating(5.0);
            riderRepository.save(rider);
            log.info("OAuth2: Created default Rider profile for {}", email);
        }

        String roleName = user.getRole() != null ? user.getRole().name() : "ROLE_RIDER";
        String token = service.generateToken(user.getEmail(), roleName);

        String encodedName = name != null ? java.net.URLEncoder.encode(name, java.nio.charset.StandardCharsets.UTF_8) : "";

        String frontEndUrl = frontendBaseUrl + "/oauth2/redirect#token=" + token
                + (!encodedName.isEmpty() ? "&name=" + encodedName : "")
                + "&isNew=" + isNew;

        response.sendRedirect(frontEndUrl);
    }
}
