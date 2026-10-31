package com.ridehailing.config;

import com.ridehailing.exception.ForbiddenException;
import com.ridehailing.exception.UnauthorizedException;
import com.ridehailing.service.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.oauth2.resourceserver.OAuth2ResourceServerSecurityMarker;
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

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry){

        registry.addEndpoint("/ws-ride")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry){

        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {

        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {

                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

                if(accessor == null){
                    return message;
                }

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
                        throw new UnauthorizedException("Error: Missing or invalid Authorization header in WebSocket CONNECT!");
                    }
                } else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
                    Principal user = accessor.getUser();

                    if(user == null){
                        throw new com.ridehailing.exception.UnauthorizedException("Must be authenticated to subscribe");
                    }

                    String destination = accessor.getDestination();

                    if(destination != null){

                        if(destination.startsWith("/topic/driver/")){
                            boolean isDriver = ((UsernamePasswordAuthenticationToken)user).getAuthorities().stream()
                                    .anyMatch(a -> a.getAuthority().equals("ROLE_DRIVER"));

                            if(!isDriver){
                                throw new ForbiddenException("Only drivers can subscribe to driver topics");
                            }
                        }
                    }
                    
                }
                return message;
            }
        });
    }
}
