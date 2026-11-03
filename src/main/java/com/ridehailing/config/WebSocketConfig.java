package com.ridehailing.config;

import com.ridehailing.exception.ForbiddenException;
import com.ridehailing.exception.UnauthorizedException;
import com.ridehailing.service.JwtService;
import com.ridehailing.service.WebSocketAuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.security.Principal;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private WebSocketAuthService webSocketAuthService;

    @Value("${frontend.url:http://localhost:5173}")
    private String frontendBaseUrl;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws-ride")
                .setAllowedOriginPatterns(frontendBaseUrl)
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                if (accessor == null) return message;

                if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                    String authHeader = accessor.getFirstNativeHeader("Authorization");
                    if (authHeader != null && authHeader.startsWith("Bearer ")) {
                        String token = authHeader.substring(7);
                        String email = jwtService.extractUsername(token);
                        if (email != null) {
                            UserDetails userDetails = userDetailsService.loadUserByUsername(email);
                            if (jwtService.isTokenValid(token, userDetails)) {
                                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                                        userDetails, null, userDetails.getAuthorities()
                                );
                                accessor.setUser(auth);
                            } else {
                                throw new UnauthorizedException("Error: Invalid JWT Token for WebSocket!");
                            }
                        } else {
                            throw new UnauthorizedException("Error: Could not extract email from token!");
                        }
                    } else {
                        throw new UnauthorizedException("Error: Missing or invalid Authorization header!");
                    }
                } else if (StompCommand.SEND.equals(accessor.getCommand())) {
                    String destination = accessor.getDestination();
                    if (destination != null && destination.startsWith("/topic/")) {
                        throw new ForbiddenException("Direct SEND to broker is blocked. Use /app/ routes.");
                    }
                } else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
                    Principal user = accessor.getUser();
                    if (user == null) {
                        throw new UnauthorizedException("Must be authenticated to subscribe");
                    }

                    String destination = accessor.getDestination();
                    if (destination != null) {
                        if (!destination.matches("^/topic/(ride|driver)/\\d+$")) {
                            throw new ForbiddenException("Invalid subscription destination pattern. Wildcards blocked.");
                        }

                        if (destination.startsWith("/topic/driver/")) {
                            String requestedDriverId = destination.substring("/topic/driver/".length());
                            if (!webSocketAuthService.canSubscribeToDriver(user.getName(), requestedDriverId)) {
                                throw new ForbiddenException("Cannot subscribe to other driver's channel");
                            }
                        } else if (destination.startsWith("/topic/ride/")) {
                            String requestedRideId = destination.substring("/topic/ride/".length());
                            if (!webSocketAuthService.canSubscribeToRide(user.getName(), requestedRideId)) {
                                throw new ForbiddenException("Not authorized to subscribe to this ride");
                            }
                        }
                    }
                }
                return message;
            }
        });
    }
}