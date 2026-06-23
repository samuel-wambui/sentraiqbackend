package com.senctraiq.Authentication;


import com.senctraiq.ApiResponse.ApiResponse;
import com.senctraiq.security.jwt.TokenRefreshRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:8080", "http://localhost:3000"})
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @RequestBody LoginRequest request,
            HttpServletRequest httpServletRequest,
            HttpServletResponse httpServletResponse
    ) {
        ApiResponse<LoginResponse> response =
                authService.login(request, httpServletRequest, httpServletResponse);

        return ResponseEntity.status(response.getStatusCode()).body(response);
    }


    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<LoginResponse>> refresh(
            @RequestBody TokenRefreshRequest request
    ) {
        ApiResponse<LoginResponse> response = authService.refreshToken(request);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @PostMapping("/logout/{username}")
    public ResponseEntity<ApiResponse<Void>> logout(
            @PathVariable String username,
            HttpServletRequest httpServletRequest
    ) {
        ApiResponse<Void> response = authService.logout(username, httpServletRequest);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @PostMapping("/password-reset/request")
    public ResponseEntity<ApiResponse<Void>> requestPasswordReset(
            @RequestBody PasswordResetRequest request
    ) {
        ApiResponse<Void> response = passwordResetService.requestPasswordReset(request);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @PostMapping("/password-reset/confirm")
    public ResponseEntity<ApiResponse<Void>> confirmPasswordReset(
            @RequestBody PasswordResetConfirmRequest request
    ) {
        ApiResponse<Void> response = passwordResetService.confirmPasswordReset(request);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @PostMapping("/temporary-password/complete")
    public ResponseEntity<ApiResponse<Void>> completeTemporaryPasswordChange(
            @RequestBody PasswordResetConfirmRequest request
    ) {
        ApiResponse<Void> response = passwordResetService.completeTemporaryPasswordChange(request);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Void>> handleServerError(IllegalStateException error) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>(error.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value(), null));
    }
}
