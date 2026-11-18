package com.ridehailing.service;

import com.ridehailing.exception.TooManyRequestsException;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OtpService {

    private static class OtpData{
        String otpCode;
        LocalDateTime expiryTime;
        int attempts;

        OtpData(String otpCode, LocalDateTime expiryTime){
            this.otpCode = otpCode;
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
        email = email.trim().toLowerCase();
        if(blockList.containsKey(email)){
            if(LocalDateTime.now().isBefore(blockList.get(email))){
                throw new TooManyRequestsException("Too many requests. Please try again after 15 minutes.");
            }
            else {
                blockList.remove(email);
            }
        }

        OtpData existingOtp = otpStorage.get(email);
        if(existingOtp != null && LocalDateTime.now().isBefore(existingOtp.expiryTime.minusMinutes(4))){
            throw new TooManyRequestsException("Please wait 60 seconds before requesting a new OTP.");
        }

        int number = secureRandom.nextInt(900000) + 100000;
        String generatedOtp = String.valueOf(number);

        LocalDateTime expiry = LocalDateTime.now().plusMinutes(5);
        otpStorage.put(email, new OtpData(generatedOtp, expiry));

        emailService.sendOtpEmail(email, generatedOtp);
    }

    public boolean verifyOtp(String email, String inputOtp){
        email = email.trim().toLowerCase();
        OtpData storedData = otpStorage.get(email);

        if(storedData == null){
            return false;
        }

        if(LocalDateTime.now().isAfter(storedData.expiryTime)){
            otpStorage.remove(email);
            return false;
        }

        if(storedData.otpCode.equals(inputOtp)) {
            otpStorage.remove(email);
            return true;
        }

        storedData.attempts++;

        if(storedData.attempts >= 5) {
            otpStorage.remove(email);
            blockList.put(email, LocalDateTime.now().plusMinutes(15));
            throw new TooManyRequestsException("Maximum OTP attempts reached. Try again in 15 minutes.");
        }

        return false;
    }
}
