package com.ridehailing.service;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OtpService {

    private static class OtpData{
        String otpCode;
        LocalDateTime expiryTime;

        OtpData(String otpCode, LocalDateTime expiryTime){
            this.otpCode = otpCode;
            this.expiryTime = expiryTime;
        }
    }

    private final ConcurrentHashMap<String, OtpData> otpStorage = new ConcurrentHashMap<>();

    private final EmailService emailService;

    public OtpService(EmailService emailService){
        this.emailService = emailService;
    }

    public void generateAndSendOtp(String email){
        String generatedOtp = String.format("%06d", new Random().nextInt(1000000));

        LocalDateTime expiry = LocalDateTime.now().plusMinutes(5);
        otpStorage.put(email, new OtpData(generatedOtp, expiry));

        emailService.sendOtpEmail(email, generatedOtp);
    }

    public boolean verifyOtp(String email, String inputOtp){
        OtpData storedData = otpStorage.get(email);

        if(storedData == null){
            return false;
        }

        if(LocalDateTime.now().isAfter(storedData.expiryTime)){
            otpStorage.remove(email);
            return false;
        }

        if(storedData.otpCode.equals(inputOtp)){
            otpStorage.remove(email);
            return true;
        }

        return false;
    }
}
