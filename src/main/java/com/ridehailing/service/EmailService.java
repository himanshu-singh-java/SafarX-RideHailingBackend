package com.ridehailing.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;

import java.util.List;
import java.util.Map;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${brevo.api.url:https://api.brevo.com/v3/smtp/email}")
    private String brevoApiUrl;

    @Value("${brevo.api.key:}")
    private String brevoApiKey;

    @Value("${brevo.sender.email:}")
    private String senderEmail;

    @Async
    public void sendOtpEmail(String to, String otp, Runnable onFailure){

        if(brevoApiKey == null || brevoApiKey.isBlank() || senderEmail == null || senderEmail.isBlank()){
            logger.error("Brevo API key or sender email is not configured.");
            if(onFailure != null){
                onFailure.run();
            }
            return;
        }

        try{
            String htmlContent = "<div style='font-family: Arial, sans-serif; padding: 20px; color: #333; border: 1px solid #ddd; border-radius: 10px; max-width: 500px;'>"
                    + "<h2 style='color: #000;'>Welcome to SafarX! 🚗</h2>"
                    + "<p>Your email verification code is:</p>"
                    + "<h1 style='color: #eab308; letter-spacing: 5px; font-size: 32px;'>" + otp + "</h1>"
                    + "<p>This code will expire in <strong>5 minutes</strong>.</p>"
                    + "<p style='font-size: 12px; color: #777; margin-top: 20px;'>If you didn't request this code, you can safely ignore this email.</p>"
                    + "</div>";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
            headers.set("api-key", brevoApiKey);

            Map<String, Object> body = Map.of(
                    "sender", Map.of("name", "SafarX", "email", senderEmail),
                    "to", List.of(Map.of("email", to)),
                    "subject", "Your SafarX verification code",
                    "htmlContent", htmlContent
            );

            HttpEntity<?> request = new HttpEntity<>(body, headers);
            restTemplate.postForEntity(brevoApiUrl, request, String.class);

            logger.info("OTP email successfully sent to: {}", maskEmail(to));
        }
        catch (Exception e){
            logger.error("Failed to send OTP email to: {}. Error: {}", maskEmail(to), e.getMessage());
            if(onFailure != null){
                onFailure.run();
            }
        }
    }

    private String maskEmail(String email){
     if(email == null || !email.contains("@")){
         return "***";
     }

     int atIndex = email.indexOf("@");

     if(atIndex <= 2){
         return "***" + email.substring(atIndex);
     }

     return email.substring(0, 2) + "***" + email.substring(atIndex);
    }
}
