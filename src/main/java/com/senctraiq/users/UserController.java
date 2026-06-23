package com.senctraiq.users;

import com.senctraiq.ApiResponse.ApiResponse;
import com.senctraiq.Authentication.PasswordResetService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:8080", "http://localhost:3000"})
public class UserController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetService passwordResetService;

    @GetMapping("/getAllUsers")
    public ResponseEntity<ApiResponse<List<User>>> getAllUsers() {
        List<User> users = userRepository.findAll()
                .stream()
                .filter(user -> !user.isDeleted())
                .toList();

        return ResponseEntity.ok(new ApiResponse<>("Users fetched successfully", HttpStatus.OK.value(), users));
    }

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<User>> createUser(@RequestBody UserRequest request) {
        validateRequiredUserFields(request, true);
        ensureUniqueUsernameAndEmail(request.getUsername(), request.getEmail(), null);

        User user = new User();
        applyUserFields(user, request);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setDeleted(false);
        user.setVerified(true);
        user.setLocked(false);

        User savedUser = userRepository.save(user);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>("User created successfully", HttpStatus.CREATED.value(), savedUser));
    }

    @PutMapping("/updateUser/{userId}")
    public ResponseEntity<ApiResponse<User>> updateUser(
            @PathVariable Long userId,
            @RequestBody UserRequest request
    ) {
        User user = getActiveUser(userId);
        validateRequiredUserFields(request, false);
        ensureUniqueUsernameAndEmail(request.getUsername(), request.getEmail(), userId);

        applyUserFields(user, request);
        User savedUser = userRepository.save(user);
        return ResponseEntity.ok(new ApiResponse<>("User updated successfully", HttpStatus.OK.value(), savedUser));
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<ApiResponse<User>> deleteUser(@PathVariable Long userId) {
        User user = getActiveUser(userId);
        user.setDeleted(true);
        user.setLoggedIn(false);
        user.getRole().clear();

        User savedUser = userRepository.save(user);
        return ResponseEntity.ok(new ApiResponse<>("User deleted successfully", HttpStatus.OK.value(), savedUser));
    }

    @PutMapping("/{userId}/suspend")
    public ResponseEntity<ApiResponse<User>> suspendUser(@PathVariable Long userId) {
        User user = getActiveUser(userId);
        user.getRole().clear();
        user.setLocked(true);
        user.setLoggedIn(false);

        User savedUser = userRepository.save(user);
        return ResponseEntity.ok(new ApiResponse<>("User suspended successfully", HttpStatus.OK.value(), savedUser));
    }

    @PutMapping("/{userId}/unlock")
    public ResponseEntity<ApiResponse<User>> unlockUser(@PathVariable Long userId) {
        User user = getActiveUser(userId);
        passwordResetService.unlockUserWithTemporaryPassword(user);

        User unlockedUser = getActiveUser(userId);
        return ResponseEntity.ok(new ApiResponse<>(
                "User unlocked and temporary password sent to their email",
                HttpStatus.OK.value(),
                unlockedUser
        ));
    }

    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @RequestBody ChangePasswordRequest request,
            Authentication authentication
    ) {
        if (authentication == null || !authentication.isAuthenticated() || isBlank(authentication.getName())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse<>("Authentication is required", HttpStatus.UNAUTHORIZED.value(), null));
        }

        User user = userRepository.findByUsernameAndDeleted(authentication.getName(), false)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        String validationMessage = validatePasswordChangeRequest(request);
        if (validationMessage != null) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(validationMessage, HttpStatus.BAD_REQUEST.value(), null));
        }

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>("Current password is incorrect", HttpStatus.BAD_REQUEST.value(), null));
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setForcePasswordChange(false);
        user.setLocked(false);
        userRepository.save(user);

        return ResponseEntity.ok(new ApiResponse<>("Password changed successfully", HttpStatus.OK.value(), null));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationError(IllegalArgumentException error) {
        return ResponseEntity.badRequest()
                .body(new ApiResponse<>(error.getMessage(), HttpStatus.BAD_REQUEST.value(), null));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Void>> handleServerError(IllegalStateException error) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>(error.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value(), null));
    }

    private User getActiveUser(Long userId) {
        return userRepository.findById(userId)
                .filter(user -> !user.isDeleted())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    private void validateRequiredUserFields(UserRequest request, boolean requirePassword) {
        if (request == null
                || isBlank(request.getUsername())
                || isBlank(request.getFirstName())
                || isBlank(request.getLastName())
                || isBlank(request.getEmail())
                || (requirePassword && isBlank(request.getPassword()))) {
            throw new IllegalArgumentException("Username, first name, last name, email, and password are required");
        }
    }

    private void ensureUniqueUsernameAndEmail(String username, String email, Long currentUserId) {
        userRepository.findByUsernameAndDeleted(username, false)
                .filter(user -> currentUserId == null || !user.getId().equals(currentUserId))
                .ifPresent(user -> {
                    throw new IllegalArgumentException("Username already exists");
                });

        userRepository.findByEmailAndDeleted(email, false)
                .filter(user -> currentUserId == null || !user.getId().equals(currentUserId))
                .ifPresent(user -> {
                    throw new IllegalArgumentException("Email already exists");
                });
    }

    private void applyUserFields(User user, UserRequest request) {
        user.setUsername(request.getUsername().trim());
        user.setPfNumber(blankToNull(request.getPfNumber()));
        user.setFirstName(request.getFirstName().trim());
        user.setLastName(request.getLastName().trim());
        user.setEmail(request.getEmail().trim());
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String blankToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }

    private String validatePasswordChangeRequest(ChangePasswordRequest request) {
        if (request == null
                || isBlank(request.getCurrentPassword())
                || isBlank(request.getNewPassword())
                || isBlank(request.getConfirmPassword())) {
            return "Current password, new password, and confirmation are required";
        }
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            return "New password and confirmation must match";
        }
        if (request.getNewPassword().length() < 6) {
            return "Password must be at least 6 characters long";
        }
        return null;
    }

    @Data
    public static class UserRequest {
        private String username;
        private String pfNumber;
        private String firstName;
        private String lastName;
        private String email;
        private String password;
    }

    @Data
    public static class ChangePasswordRequest {
        private String username;
        private String currentPassword;
        private String newPassword;
        private String confirmPassword;
    }
}
