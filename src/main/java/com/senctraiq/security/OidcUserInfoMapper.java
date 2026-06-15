package com.senctraiq.security;


import com.senctraiq.users.User;
import com.senctraiq.users.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class OidcUserInfoMapper {

    private final UserRepository userRepository;

    public OidcUserInfoMapper(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public OidcUserInfo loadUserInfo(String username) {
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("[SSO /userinfo] Called for username: {}", username);

        User user = userRepository.findByUsernameAndDeleted(username, false)
                .orElseThrow(() -> {
                    log.error("[SSO /userinfo] User not found: {}", username);
                    return new IllegalStateException("User not found: " + username);
                });

        List<String> roles = new ArrayList<>(user.getRole().stream()
                .map(role -> role.getName())
                .toList());

        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", user.getUsername());
        claims.put("preferred_username", user.getUsername());
        claims.put("name", user.getFirstName() + " " + user.getLastName());
        claims.put("email", user.getEmail());
        claims.put("roles", roles);

        log.info("[SSO /userinfo] Returning — sub: {}, email: {}, name: {}, roles: {}",
                user.getUsername(), user.getEmail(),
                user.getFirstName() + " " + user.getLastName(), roles);
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        return new OidcUserInfo(claims);
    }
}
