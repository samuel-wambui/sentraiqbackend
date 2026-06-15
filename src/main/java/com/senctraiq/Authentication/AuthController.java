package com.senctraiq.Authentication;


import com.senctraiq.ApiResponse.ApiResponse;
import com.senctraiq.security.jwt.TokenRefreshRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:8080", "http://localhost:3000"})
public class AuthController {

    private final AuthService authService;

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
}
