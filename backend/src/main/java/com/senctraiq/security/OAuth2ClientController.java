package com.senctraiq.security;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.web.bind.annotation.*;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/oauth2/clients")
public class OAuth2ClientController {

    private final RegisteredClientRepository registeredClientRepository;
    private final PasswordEncoder passwordEncoder;

    public OAuth2ClientController(RegisteredClientRepository registeredClientRepository,
                                   PasswordEncoder passwordEncoder) {
        this.registeredClientRepository = registeredClientRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Data
    public static class ClientRegistrationRequest {
        private String clientId;
        private String clientSecret;
        private String clientName;
        private List<String> redirectUris;
        private List<String> scopes;
        private List<String> grantTypes;
        private boolean requireConsent = false;
        private int accessTokenTtlMinutes = 30;
        private int refreshTokenTtlHours = 24;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody ClientRegistrationRequest req) {
        log.info("[OAuth2 Client] Registering new client: {}", req.getClientId());

        if (registeredClientRepository.findByClientId(req.getClientId()) != null) {
            log.warn("[OAuth2 Client] Client already exists: {}", req.getClientId());
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "Client ID already exists: " + req.getClientId(),
                    "statusCode", 400
            ));
        }

        // Generate secure random secret if not provided
        String rawSecret = (req.getClientSecret() != null && !req.getClientSecret().isBlank())
                ? req.getClientSecret()
                : generateSecret();

        String hashedSecret = "{bcrypt}" + passwordEncoder.encode(rawSecret);

        RegisteredClient.Builder builder = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId(req.getClientId())
                .clientSecret(hashedSecret)
                .clientName(req.getClientName() != null ? req.getClientName() : req.getClientId())
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
                .clientSettings(ClientSettings.builder()
                        .requireAuthorizationConsent(req.isRequireConsent())
                        .build())
                .tokenSettings(TokenSettings.builder()
                        .accessTokenTimeToLive(Duration.ofMinutes(req.getAccessTokenTtlMinutes()))
                        .refreshTokenTimeToLive(Duration.ofHours(req.getRefreshTokenTtlHours()))
                        .reuseRefreshTokens(true)
                        .build());

        // Redirect URIs
        if (req.getRedirectUris() != null) {
            req.getRedirectUris().forEach(builder::redirectUri);
        }

        // Scopes
        List<String> scopes = req.getScopes() != null ? req.getScopes() : List.of(OidcScopes.OPENID, OidcScopes.PROFILE, OidcScopes.EMAIL);
        scopes.forEach(builder::scope);

        // Grant types
        List<String> grantTypes = req.getGrantTypes() != null ? req.getGrantTypes() : List.of("authorization_code", "refresh_token");
        for (String gt : grantTypes) {
            switch (gt.toLowerCase()) {
                case "authorization_code" -> builder.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE);
                case "refresh_token" -> builder.authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN);
                case "client_credentials" -> builder.authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS);
            }
        }

        registeredClientRepository.save(builder.build());
        log.info("[OAuth2 Client] Registered successfully: {}", req.getClientId());

        return ResponseEntity.ok(Map.of(
                "message", "Client registered successfully",
                "statusCode", 200,
                "entity", Map.of(
                        "clientId", req.getClientId(),
                        "clientSecret", rawSecret,
                        "authUrl", "http://localhost:8081/oauth2/authorize",
                        "tokenUrl", "http://localhost:8081/oauth2/token",
                        "userInfoUrl", "http://localhost:8081/userinfo",
                        "scopes", scopes,
                        "grantTypes", grantTypes,
                        "note", "Save the clientSecret now — it cannot be retrieved again"
                )
        ));
    }

    @GetMapping("/registeredClients")
    public ResponseEntity<?> listClients() {
        // Note: JdbcRegisteredClientRepository doesn't have a findAll method
        // Return instructions instead
        return ResponseEntity.ok(Map.of(
                "message", "Query oauth2_registered_client table for all clients",
                "statusCode", 200,
                "entity", Map.of(
                        "authUrl", "http://localhost:8081/oauth2/authorize",
                        "tokenUrl", "http://localhost:8081/oauth2/token",
                        "userInfoUrl", "http://localhost:8081/userinfo",
                        "jwksUrl", "http://localhost:8081/oauth2/jwks",
                        "discoveryUrl", "http://localhost:8081/.well-known/openid-configuration"
                )
        ));
    }

    @DeleteMapping("/{clientId}")
    public ResponseEntity<?> deleteClient(@PathVariable String clientId) {
        RegisteredClient existing = registeredClientRepository.findByClientId(clientId);
        if (existing == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "Client not found: " + clientId,
                    "statusCode", 404
            ));
        }
        if ("grafana".equals(clientId)) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "Cannot delete the grafana client",
                    "statusCode", 400
            ));
        }
        // JdbcRegisteredClientRepository doesn't expose delete — use JDBC directly
        log.warn("[OAuth2 Client] Delete requested for: {} — use DB directly: DELETE FROM oauth2_registered_client WHERE client_id='{}'", clientId, clientId);
        return ResponseEntity.ok(Map.of(
                "message", "Run: DELETE FROM oauth2_registered_client WHERE client_id='" + clientId + "'",
                "statusCode", 200
        ));
    }

    private String generateSecret() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
