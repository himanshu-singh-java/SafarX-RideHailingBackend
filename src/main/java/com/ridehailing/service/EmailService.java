package com.ridehailing.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender javaMailSender;

    @Value("${spring.mail.username}")
    private String senderEmail;

    public EmailService(JavaMailSender javaMailSender){
        this.javaMailSender = javaMailSender;
    }

    @Async
    public void sendOtpEmail(String to, String otp, Runnable onFailure){
        try{
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(senderEmail);
            helper.setTo(to);
            helper.setSubject("Your SafarX verification code");

            String htmlContent = "<div style='font-family: Arial, sans-serif; padding: 20px; color: #333; border: 1px solid #ddd; border-radius: 10px; max-width: 500px;'>"
                    + "<h2 style='color: #000;'>Welcome to SafarX! 🚗</h2>"
                    + "<p>Your email verification code is:</p>"
                    + "<h1 style='color: #eab308; letter-spacing: 5px; font-size: 32px;'>" + otp + "</h1>"
                    + "<p>This code will expire in <strong>5 minutes</strong>.</p>"
                    + "<p style='font-size: 12px; color: #777; margin-top: 20px;'>If you didn't request this code, you can safely ignore this email.</p>"
                    + "</div>";

            helper.setText(htmlContent, true);
            javaMailSender.send(message);
            logger.info("OTP email successfully sent to: {}", maskEmail(to));
        }
        catch (MessagingException | MailException m){
            logger.error("Failed to send OTP email to: {}. Error: {}", maskEmail(to), m.getMessage());
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
