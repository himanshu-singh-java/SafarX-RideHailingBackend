package com.ridehailing.filter;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import com.ridehailing.service.JwtService;
import com.ridehailing.service.MyUserDetailsService;

import java.io.IOException;

@Slf4j
@Component
public class JwtFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    private final MyUserDetailsService myUserDetailsService;

    public JwtFilter(JwtService jwtService, MyUserDetailsService myUserDetailsService){
        this.jwtService = jwtService;
        this.myUserDetailsService = myUserDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        Claims claims = null;
        String username = null;

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                claims = jwtService.extractAllClaims(token);
                username = claims.getSubject();
            } catch (Exception e) {
                log.warn("JWT validation failed: {}", e.getMessage());
            }
        }

        if (username != null && claims != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                UserDetails userDetails = myUserDetailsService.loadUserByUsername(username);

                if (jwtService.isTokenValid(claims, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities()
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            } catch (Exception e) {
                log.warn("User authentication failed: {}", e.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }
}