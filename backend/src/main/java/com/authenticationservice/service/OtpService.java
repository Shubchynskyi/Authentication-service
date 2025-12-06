package com.authenticationservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for generating and validating OTP (One-Time Password) codes.
 * OTP codes expire after 10 minutes.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OtpService {

    private static final int OTP_LENGTH = 6;
    private static final int OTP_EXPIRY_MINUTES = 10;
    private final SecureRandom random = new SecureRandom();
    
    // In-memory storage: email -> OTP data (code, expiry time)
    private final Map<String, OtpData> otpStorage = new ConcurrentHashMap<>();

    /**
     * Generates and stores OTP code for given email.
     * 
     * @param email Email address
     * @return Generated OTP code
     */
    public String generateOtp(String email) {
        String otp = generateRandomOtp();
        LocalDateTime expiryTime = LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES);
        
        otpStorage.put(email, new OtpData(otp, expiryTime));
        log.info("Generated OTP for email: {} (expires in {} minutes)", email, OTP_EXPIRY_MINUTES);
        
        return otp;
    }

    /**
     * Validates OTP code for given email.
     * 
     * @param email Email address
     * @param otp OTP code to validate
     * @return true if OTP is valid and not expired, false otherwise
     */
    public boolean validateOtp(String email, String otp) {
        OtpData otpData = otpStorage.get(email);
        
        if (otpData == null) {
            log.warn("No OTP found for email: {}", email);
            return false;
        }
        
        if (LocalDateTime.now().isAfter(otpData.expiryTime)) {
            log.warn("OTP expired for email: {}", email);
            otpStorage.remove(email);
            return false;
        }
        
        boolean isValid = otpData.code.equals(otp);
        if (isValid) {
            otpStorage.remove(email);
            log.info("OTP validated successfully for email: {}", email);
        } else {
            log.warn("Invalid OTP provided for email: {}", email);
        }
        
        return isValid;
    }

    /**
     * Removes OTP for given email (cleanup after use or expiry).
     * 
     * @param email Email address
     */
    public void removeOtp(String email) {
        otpStorage.remove(email);
    }

    private String generateRandomOtp() {
        StringBuilder otp = new StringBuilder(OTP_LENGTH);
        for (int i = 0; i < OTP_LENGTH; i++) {
            otp.append(random.nextInt(10));
        }
        return otp.toString();
    }

    private static class OtpData {
        final String code;
        final LocalDateTime expiryTime;

        OtpData(String code, LocalDateTime expiryTime) {
            this.code = code;
            this.expiryTime = expiryTime;
        }
    }
}

