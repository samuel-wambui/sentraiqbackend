package com.senctraiq.Authentication;


import com.senctraiq.ApiResponse.ApiResponse;
import com.senctraiq.roles.Role;
import com.senctraiq.security.jwt.JwtService;
import com.senctraiq.security.jwt.TokenRefreshRequest;
import com.senctraiq.users.User;
import com.senctraiq.users.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@lombok.extern.slf4j.Slf4j
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;

    // In-memory tracker for failed attempts. Thread-safe.
    private final ConcurrentMap<String, Integer> failedAttempts = new ConcurrentHashMap<>();
    private static final int MAX_FAILED_ATTEMPTS = 3;

    public ApiResponse<LoginResponse> login(LoginRequest request,
                                            HttpServletRequest httpRequest,
                                            HttpServletResponse httpResponse) {
        String username = request.getUsername();
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("[LOGIN] Attempt for username: {} | IP: {}", username, httpRequest.getRemoteAddr());

        var optionalUser = userRepository.findByUsernameAndDeleted(username, false);
        if (optionalUser.isEmpty()) {
            return new ApiResponse<>("Invalid username or password", 401, null);
        }

        User user = optionalUser.get();

        if (user.isLocked()) {
            return new ApiResponse<>("Account is locked due to too many failed login attempts. Contact admin or reset password.", 423, null);
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, request.getPassword())
            );

            failedAttempts.remove(username);
            user.setLoggedIn(true);
            if (user.isLocked()) {
                user.setLocked(false);
            }
            userRepository.save(user);

            UserDetails userDetails = (UserDetails) authentication.getPrincipal();

            // ✅ Create Spring Security session for OIDC
            UsernamePasswordAuthenticationToken sessionAuth =
                    new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );

            SecurityContextHolder.getContext().setAuthentication(sessionAuth);
            var session = httpRequest.getSession(true);
            session.setAttribute(
                    HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                    SecurityContextHolder.getContext()
            );

            List<String> authorities = userDetails.getAuthorities()
                    .stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toList());

            List<String> roles = user.getRole().stream()
                    .map(Role::getName)
                    .collect(Collectors.toList());

            log.info("[LOGIN] Success — user: {} | roles: {} | JSESSIONID: {}", username, roles, session.getId());

            String accessToken = jwtService.generateToken(userDetails.getUsername(), authorities, roles);
            String refreshToken = jwtService.generateRefreshToken(userDetails.getUsername(), authorities);

            LoginResponse response = new LoginResponse(
                    accessToken,
                    refreshToken,
                    userDetails.getUsername(),
                    user.getFirstName(),
                    user.getEmail(),
                    user.getLastName(),
                    user.getLoggedIn(),
                    user.getIsOnShift(),
                    user.getIsOnLeave()
            );

            return new ApiResponse<>("Login successful", 200, response);

        } catch (BadCredentialsException ex) {
            int attempts = failedAttempts.getOrDefault(username, 0) + 1;
            failedAttempts.put(username, attempts);
            log.warn("[LOGIN] Failed for: {} | attempt {}/{}", username, attempts, MAX_FAILED_ATTEMPTS);

            if (attempts >= MAX_FAILED_ATTEMPTS) {
                user.setLocked(true);
                userRepository.save(user);
                failedAttempts.remove(username);
                return new ApiResponse<>("Account locked due to too many failed login attempts.", 423, null);
            } else {
                int remaining = MAX_FAILED_ATTEMPTS - attempts;
                return new ApiResponse<>("Invalid username or password. " + remaining + " attempt(s) remaining.", 401, null);
            }
        } catch (Exception ex) {
            return new ApiResponse<>("An error occurred during login: " + ex.getMessage(), 500, null);
        }
    }


    public ApiResponse<LoginResponse> refreshToken(TokenRefreshRequest request) {
        String refreshToken = request != null ? request.getRefreshToken() : null;
        if (refreshToken == null || refreshToken.isBlank()) {
            return new ApiResponse<>("Refresh token is required", 400, null);
        }

        try {
            if (!jwtService.validateRefreshToken(refreshToken)) {
                return new ApiResponse<>("Refresh token expired", 401, null);
            }

            String username = jwtService.extractUserName(refreshToken);
            var optionalUser = userRepository.findByUsernameAndDeleted(username, false);
            if (optionalUser.isEmpty()) {
                return new ApiResponse<>("User not found", 404, null);
            }

            User user = optionalUser.get();
            if (user.isLocked()) {
                return new ApiResponse<>("Account is locked", 423, null);
            }

            user.setLoggedIn(true);
            userRepository.save(user);

            List<String> authorities = user.getRole().stream()
                    .flatMap(role -> role.getAuthorities().stream())
                    .map(GrantedAuthority::getAuthority)
                    .distinct()
                    .collect(Collectors.toList());

            List<String> roles = user.getRole().stream()
                    .map(Role::getName)
                    .collect(Collectors.toList());

            String accessToken = jwtService.generateToken(username, authorities, roles);
            String newRefreshToken = jwtService.generateRefreshToken(username, authorities);

            LoginResponse response = new LoginResponse(
                    accessToken,
                    newRefreshToken,
                    user.getUsername(),
                    user.getFirstName(),
                    user.getEmail(),
                    user.getLastName(),
                    user.getLoggedIn(),
                    user.getIsOnShift(),
                    user.getIsOnLeave()
            );

            return new ApiResponse<>("Token refreshed", 200, response);
        } catch (Exception ex) {
            return new ApiResponse<>("Invalid refresh token", 401, null);
        }
    }

    public ApiResponse<Void> logout(String username, HttpServletRequest httpRequest) {
        if (username != null && !username.isBlank()) {
            userRepository.findByUsernameAndDeleted(username, false).ifPresent(user -> {
                user.setLoggedIn(false);
                userRepository.save(user);
            });
        }

        SecurityContextHolder.clearContext();
        var session = httpRequest.getSession(false);
        if (session != null) {
            session.invalidate();
        }

        return new ApiResponse<>("Logout successful", 200, null);
    }
}
