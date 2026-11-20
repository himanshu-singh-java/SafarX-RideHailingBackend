package com.ridehailing.service;

import com.ridehailing.exception.TooManyRequestsException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OtpService {

    private static final int MAX_ATTEMPTS = 5;
    private static final int OTP_EXPIRY_MINUTES = 5;
    private static final int RESEND_COOLDOWN_SECONDS = 60;
    private static final int BLOCK_MINUTES = 15;

    private static class OtpData{
        final String otpCode;
        final LocalDateTime createdAt;
        final LocalDateTime expiryTime;
        int attempts;

        OtpData(String otpCode,LocalDateTime createdAt, LocalDateTime expiryTime){
            this.otpCode = otpCode;
            this.createdAt = createdAt;
            this.expiryTime = expiryTime;
            this.attempts = 0;
        }
    }

    private final ConcurrentHashMap<String, OtpData> otpStorage = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, LocalDateTime> blockList = new ConcurrentHashMap<>();

    private final SecureRandom secureRandom = new SecureRandom();
    private final EmailService emailService;

    public OtpService(EmailService emailService){
        this.emailService = emailService;
    }

    public void generateAndSendOtp(String email){
        if(email == null || email.isBlank()){
            throw new IllegalArgumentException("Email is required.");
        }

        String normalizedEmail = email.trim().toLowerCase();
        LocalDateTime now = LocalDateTime.now();

        LocalDateTime blockedUntil = blockList.get(normalizedEmail);

        if(blockedUntil != null){
            if(now.isBefore(blockedUntil)){
                throw new TooManyRequestsException("Too many requests. Please try again after 15 minutes.");
            }
            blockList.remove(normalizedEmail, blockedUntil);
        }

        int number = secureRandom.nextInt(900000) + 100000;
        String generatedOtp = String.valueOf(number);
        OtpData newOtp = new OtpData(generatedOtp, now, now.plusMinutes(OTP_EXPIRY_MINUTES));
        boolean[] cooldownActive = {false};

        otpStorage.compute(normalizedEmail, (key, existing) -> {
            if(existing != null && now.isBefore(existing.createdAt.plusSeconds(RESEND_COOLDOWN_SECONDS))){
                cooldownActive[0] = true;
                return existing;
            }
            return newOtp;
        });

        if(cooldownActive[0]){
            throw new TooManyRequestsException("Please wait 60 seconds before requesting a new OTP.");
        }

        emailService.sendOtpEmail(normalizedEmail, generatedOtp, () -> otpStorage.remove(normalizedEmail, newOtp));
    }

    public boolean verifyOtp(String email, String inputOtp){

        if (email == null || email.isBlank() || inputOtp == null || inputOtp.isBlank()) {
            return false;
        }

        String normalizedEmail = email.trim().toLowerCase();
        String cleanInput = inputOtp.trim();
        LocalDateTime now = LocalDateTime.now();

        LocalDateTime blockedUntil = blockList.get(normalizedEmail);

        if(blockedUntil != null){
            if(now.isBefore(blockedUntil)){
                throw new TooManyRequestsException("Maximum OTP attempts reached. Try again in 15 minutes.");
            }
            blockList.remove(normalizedEmail, blockedUntil);
        }

        boolean[] matched = {false};
        boolean[] newlyBlocked = {false};

        otpStorage.compute(normalizedEmail, (key, storedData) -> {
            if(storedData == null || now.isAfter(storedData.expiryTime)){
                return null;
            }

            boolean isEqual = MessageDigest.isEqual(
                    storedData.otpCode.getBytes(StandardCharsets.UTF_8),
                    cleanInput.getBytes(StandardCharsets.UTF_8)
            );

            if(isEqual){
                matched[0] = true;
                return null;
            }

            storedData.attempts++;

            if(storedData.attempts >= MAX_ATTEMPTS){
                blockList.put(normalizedEmail, now.plusMinutes(BLOCK_MINUTES));
                newlyBlocked[0] = true;
                return null;
            }

            return storedData;
        });

        if(newlyBlocked[0]){
            throw new TooManyRequestsException("Maximum OTP attempts reached. Try again in 15 minutes.");
        }

        return matched[0];
    }

    @Scheduled(fixedRate = 300_000)
    public void cleanupExpiredEntries(){
        LocalDateTime now = LocalDateTime.now();
        otpStorage.entrySet().removeIf(entry -> now.isAfter(entry.getValue().expiryTime));
        blockList.entrySet().removeIf(entry -> now.isAfter(entry.getValue()));
    }
}
