package com.ridehailing.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {

    @Value("${jwt.secret:${jwt_secret}}")
    private String secretKey;

    private SecretKey signingKey;

    @PostConstruct
    public void init(){
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    public SecretKey getKey(){
        if(this.signingKey == null){
            init();
        }
        return this.signingKey;
    }

    public String generateToken(String username){
        return generateToken(username, "ROLE_RIDER");
    }

    public String generateToken(String username, String role){
        Map<String, Object> claims = new HashMap<>();
        if (role != null) {
            claims.put("role", role);
        }

        long now = System.currentTimeMillis();

        return Jwts.builder()
                .claims(claims)
                .subject(username)
                .issuedAt(new Date(now))
                .expiration(new Date(now + 1000L * 60 * 60 * 24))
                .signWith(getKey())
                .compact();
    }

    public boolean isTokenValid(Claims claims, UserDetails userDetails){
        final String username = claims.getSubject();
        return username != null
                && (username.equals(userDetails.getUsername()))
                && !claims.getExpiration().before(new Date());
    }

    public boolean isTokenValid(String token, UserDetails userDetails){
        final Claims claims = extractAllClaims(token);
        return isTokenValid(claims, userDetails);
    }

    public String extractUsername(String token) {
        return extractClaims(token, Claims::getSubject);
    }

    private <T> T extractClaims(String token, Function<Claims, T> claimsResolver){
        final Claims claims= extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public Claims extractAllClaims(String token){

        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
