package com.ridehailing.config;

import com.ridehailing.model.Role;
import com.ridehailing.model.Users;
import com.ridehailing.repository.UserRepository;
import com.ridehailing.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Optional;

@Component
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService service;

    @Value("${frontend.url:http://localhost:5173}")
    private String frontendBaseUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, FilterChain chain, Authentication authentication) throws IOException, ServletException {
        AuthenticationSuccessHandler.super.onAuthenticationSuccess(request, response, chain, authentication);
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");

        Optional<Users> existingUser = userRepository.findByEmail(email);
        boolean isNew = existingUser.isEmpty();
        Users user;

        if(isNew){
            user = new Users();
            user.setEmail(email);
            user.setPassword("");
            user.setRole(Role.ROLE_RIDER);
            user.setProvider("GOOGLE");
            userRepository.save(user);
        }
        else{
            user = existingUser.get();

            if("LOCAL".equals(user.getProvider())){
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Email already registered with password. Please login using credentials.");
                return;
            }
        }

        String roleName = user.getRole() != null ? user.getRole().name() : "ROLE_RIDER";
        String token = service.generateToken(user.getEmail(), roleName);

        String name = oAuth2User.getAttribute("name");
        String encodedName = name != null ? java.net.URLEncoder.encode(name, java.nio.charset.StandardCharsets.UTF_8) : "";

        String frontEndUrl = frontendBaseUrl + "/oauth2/redirect#token=" + token
                + (!encodedName.isEmpty() ? "&name=" + encodedName : "")
                + "&isNew=" + isNew;

        response.sendRedirect(frontEndUrl);
    }
}
