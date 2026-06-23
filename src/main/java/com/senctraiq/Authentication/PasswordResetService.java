package com.senctraiq.Authentication;

import com.senctraiq.ApiResponse.ApiResponse;
import com.senctraiq.users.User;
import com.senctraiq.users.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private static final String PASSWORD_RESET_PURPOSE = "PASSWORD_RESET";
    private static final String TEMPORARY_PASSWORD_PURPOSE = "TEMPORARY_PASSWORD";
    private static final Duration PASSWORD_RESET_TTL = Duration.ofMinutes(30);
    private static final Duration TEMPORARY_PASSWORD_TTL = Duration.ofMinutes(10);
    private static final int MIN_PASSWORD_LENGTH = 6;
    private static final char[] UPPER = "ABCDEFGHJKLMNPQRSTUVWXYZ".toCharArray();
    private static final char[] LOWER = "abcdefghijkmnopqrstuvwxyz".toCharArray();
    private static final char[] DIGITS = "23456789".toCharArray();
    private static final char[] SPECIAL = "!@#$%".toCharArray();

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordMailService passwordMailService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public ApiResponse<Void> requestPasswordReset(PasswordResetRequest request) {
        String identifier = resolveIdentifier(request);
        if (isBlank(identifier)) {
            return new ApiResponse<>("Enter your username or email address", HttpStatus.BAD_REQUEST.value(), null);
        }

        findActiveUserByIdentifier(identifier).ifPresent(user -> {
            markActiveTokensUsed(user, PASSWORD_RESET_PURPOSE);
            String token = createToken(user, PASSWORD_RESET_PURPOSE, PASSWORD_RESET_TTL);
            passwordMailService.sendPasswordResetLink(user, token);
        });

        return new ApiResponse<>("If the account exists, a password reset link has been sent.", HttpStatus.OK.value(), null);
    }

    @Transactional
    public ApiResponse<Void> confirmPasswordReset(PasswordResetConfirmRequest request) {
        PasswordValidation validation = validatePasswordRequest(request);
        if (!validation.valid()) {
            return new ApiResponse<>(validation.message(), HttpStatus.BAD_REQUEST.value(), null);
        }

        Optional<PasswordResetToken> optionalToken = tokenRepository.findByTokenHashAndPurposeAndUsedAtIsNull(
                hashToken(request.getToken()),
                PASSWORD_RESET_PURPOSE
        );
        if (optionalToken.isEmpty()) {
            return new ApiResponse<>("Password reset link is invalid or already used.", HttpStatus.BAD_REQUEST.value(), null);
        }

        PasswordResetToken token = optionalToken.get();
        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            token.setUsedAt(LocalDateTime.now());
            tokenRepository.save(token);
            return new ApiResponse<>("Password reset link has expired.", HttpStatus.GONE.value(), null);
        }

        User user = token.getUser();
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setLocked(false);
        user.setForcePasswordChange(false);
        user.setVerified(true);
        user.setLoggedIn(false);
        userRepository.save(user);
        markActiveTokensUsed(user, PASSWORD_RESET_PURPOSE);

        return new ApiResponse<>("Password reset successfully. You can now sign in.", HttpStatus.OK.value(), null);
    }

    @Transactional
    public void unlockUserWithTemporaryPassword(User user) {
        String temporaryPassword = generateTemporaryPassword();

        markActiveTokensUsed(user, TEMPORARY_PASSWORD_PURPOSE);
        user.setPassword(passwordEncoder.encode(temporaryPassword));
        user.setLocked(false);
        user.setForcePasswordChange(true);
        user.setLoggedIn(false);
        userRepository.save(user);

        passwordMailService.sendTemporaryPassword(user, temporaryPassword);
    }

    @Transactional
    public String createTemporaryPasswordChallenge(User user) {
        markActiveTokensUsed(user, TEMPORARY_PASSWORD_PURPOSE);
        return createToken(user, TEMPORARY_PASSWORD_PURPOSE, TEMPORARY_PASSWORD_TTL);
    }

    @Transactional
    public ApiResponse<Void> completeTemporaryPasswordChange(PasswordResetConfirmRequest request) {
        PasswordValidation validation = validatePasswordRequest(request);
        if (!validation.valid()) {
            return new ApiResponse<>(validation.message(), HttpStatus.BAD_REQUEST.value(), null);
        }

        Optional<PasswordResetToken> optionalToken = tokenRepository.findByTokenHashAndPurposeAndUsedAtIsNull(
                hashToken(request.getToken()),
                TEMPORARY_PASSWORD_PURPOSE
        );
        if (optionalToken.isEmpty()) {
            return new ApiResponse<>("Temporary password session is invalid or already used.", HttpStatus.BAD_REQUEST.value(), null);
        }

        PasswordResetToken token = optionalToken.get();
        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            token.setUsedAt(LocalDateTime.now());
            tokenRepository.save(token);
            return new ApiResponse<>("Temporary password session has expired. Ask an admin to unlock the account again.", HttpStatus.GONE.value(), null);
        }

        User user = token.getUser();
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setLocked(false);
        user.setForcePasswordChange(false);
        user.setLoggedIn(false);
        userRepository.save(user);
        markActiveTokensUsed(user, TEMPORARY_PASSWORD_PURPOSE);

        return new ApiResponse<>("Password changed successfully. You can now sign in.", HttpStatus.OK.value(), null);
    }

    private String resolveIdentifier(PasswordResetRequest request) {
        if (request == null) return null;
        if (!isBlank(request.getIdentifier())) return request.getIdentifier().trim();
        if (!isBlank(request.getUsername())) return request.getUsername().trim();
        if (!isBlank(request.getEmail())) return request.getEmail().trim();
        return null;
    }

    private Optional<User> findActiveUserByIdentifier(String identifier) {
        String value = identifier.trim();
        Optional<User> byUsername = userRepository.findByUsernameAndDeleted(value, false);
        if (byUsername.isPresent()) return byUsername;
        return userRepository.findByEmailAndDeleted(value, false);
    }

    private String createToken(User user, String purpose, Duration ttl) {
        String rawToken = generateRawToken();

        PasswordResetToken token = new PasswordResetToken();
        token.setUser(user);
        token.setPurpose(purpose);
        token.setTokenHash(hashToken(rawToken));
        token.setExpiresAt(LocalDateTime.now().plus(ttl));
        tokenRepository.save(token);

        return rawToken;
    }

    private void markActiveTokensUsed(User user, String purpose) {
        LocalDateTime usedAt = LocalDateTime.now();
        List<PasswordResetToken> tokens = tokenRepository.findByUserAndPurposeAndUsedAtIsNull(user, purpose);
        tokens.forEach(token -> token.setUsedAt(usedAt));
        tokenRepository.saveAll(tokens);
    }

    private String generateRawToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String generateTemporaryPassword() {
        char[] all = (new String(UPPER) + new String(LOWER) + new String(DIGITS) + new String(SPECIAL)).toCharArray();
        List<Character> passwordChars = new ArrayList<>();
        passwordChars.add(randomChar(UPPER));
        passwordChars.add(randomChar(LOWER));
        passwordChars.add(randomChar(DIGITS));
        passwordChars.add(randomChar(SPECIAL));

        for (int index = passwordChars.size(); index < 14; index++) {
            passwordChars.add(randomChar(all));
        }

        java.util.Collections.shuffle(passwordChars, secureRandom);
        StringBuilder password = new StringBuilder();
        passwordChars.forEach(password::append);
        return password.toString();
    }

    private char randomChar(char[] chars) {
        return chars[secureRandom.nextInt(chars.length)];
    }

    private PasswordValidation validatePasswordRequest(PasswordResetConfirmRequest request) {
        if (request == null || isBlank(request.getToken())) {
            return new PasswordValidation(false, "Password reset token is required");
        }
        if (isBlank(request.getNewPassword()) || isBlank(request.getConfirmPassword())) {
            return new PasswordValidation(false, "New password and confirmation are required");
        }
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            return new PasswordValidation(false, "New password and confirmation must match");
        }
        if (request.getNewPassword().length() < MIN_PASSWORD_LENGTH) {
            return new PasswordValidation(false, "Password must be at least 6 characters long");
        }
        return new PasswordValidation(true, null);
    }

    private String hashToken(String token) {
        if (token == null) return "";
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                String hexByte = Integer.toHexString(b & 0xff);
                if (hexByte.length() == 1) {
                    hex.append('0');
                }
                hex.append(hexByte);
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException("SHA-256 is not available", error);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private record PasswordValidation(boolean valid, String message) {
    }
}
