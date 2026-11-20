package com.ridehailing.filter;

import com.ridehailing.service.RateLimitService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class AuthRateLimitFilter extends OncePerRequestFilter {

    private static final int OTP_MAX_REQUESTS = 5;
    private static final long OTP_WINDOW_MS = 10 * 60 * 1000L;

    private static final int LOGIN_MAX_REQUESTS = 10;
    private static final long LOGIN_WINDOW_MS = 5 * 60 * 1000L;

    private final RateLimitService rateLimitService;

    public AuthRateLimitFilter(RateLimitService rateLimitService){
        this.rateLimitService = rateLimitService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        if(!"POST".equalsIgnoreCase(request.getMethod())){
            filterChain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();
        String clientIp = extractClientIp(request);

        if("/api/auth/send-otp".equals(path)){
            if(!rateLimitService.isAllowed("otp:" + clientIp, OTP_MAX_REQUESTS, OTP_WINDOW_MS)){
                writeTooManyRequests(response, "Too many OTP requests from this IP. Please try again later.");
                return;
            }
        } else if ("/api/auth/login".equals(path)) {
            if(!rateLimitService.isAllowed("login:" + clientIp, LOGIN_MAX_REQUESTS, LOGIN_WINDOW_MS)){
                writeTooManyRequests(response, "Too many login attempts. Please try again after a few minutes.");
                return;
            }
        }
        filterChain.doFilter(request, response);
    }

    private String extractClientIp(HttpServletRequest request){
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if(forwardedFor != null && !forwardedFor.isBlank()){
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private void writeTooManyRequests(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType("application/json");
        response.getWriter().write(
                "{\"error\":\"Too Many Requests\",\"message\":\"" + message + "\",\"status\":429}"
        );
    }
}
