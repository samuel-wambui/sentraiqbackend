package com.senctraiq.dashboard;

import com.senctraiq.ApiResponse.ApiResponse;
import com.senctraiq.users.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    private final UserRepository userRepository;

    public DashboardController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

//    @GetMapping("/summary")
////    public ApiResponse<Map<String, Object>> summary(Authentication authentication) {
//        return ApiResponse.ok("Dashboard summary", Map.of(
//                "system", "SenctraIQ",
//                "signedInAs", authentication.getName(),
//                "registeredUsers", userRepository.count(),
//                "authMode", "JWT + OAuth2/OIDC SSO",
//                "generatedAt", LocalDateTime.now().toString()
//        ));
//    }
}
