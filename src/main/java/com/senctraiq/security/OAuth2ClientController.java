package com.senctraiq.security;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
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
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/oauth2/clients")
public class OAuth2ClientController {

    private final RegisteredClientRepository registeredClientRepository;
    private final RegisteredClientEntityRepository registeredClientEntityRepository;
    private final PasswordEncoder passwordEncoder;

    public OAuth2ClientController(RegisteredClientRepository registeredClientRepository,
                                  RegisteredClientEntityRepository registeredClientEntityRepository,
                                  PasswordEncoder passwordEncoder) {
        this.registeredClientRepository = registeredClientRepository;
        this.registeredClientEntityRepository = registeredClientEntityRepository;
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
        String clientId = clean(req.getClientId());
        log.info("[OAuth2 Client] Registering new client: {}", clientId);

        if (clientId.isBlank()) {
            return badRequest("Client ID is required");
        }

        RegisteredClientEntity existingEntity = registeredClientEntityRepository.findByClientId(clientId).orElse(null);
        if (existingEntity != null && !existingEntity.isDeleted()) {
            log.warn("[OAuth2 Client] Client already exists: {}", clientId);
            return badRequest("Client ID already exists: " + clientId);
        }

        String rawSecret = clean(req.getClientSecret()).isBlank() ? generateSecret() : req.getClientSecret();
        String hashedSecret = "{bcrypt}" + passwordEncoder.encode(rawSecret);

        req.setClientId(clientId);
        String id = existingEntity != null && existingEntity.isDeleted()
                ? existingEntity.getId()
                : UUID.randomUUID().toString();

        List<String> scopes = normalizeScopes(req.getScopes());
        List<String> grantTypes = normalizeGrantTypes(req.getGrantTypes());
        registeredClientRepository.save(buildRegisteredClient(id, req, hashedSecret, scopes, grantTypes));

        log.info("[OAuth2 Client] Registered successfully: {}", clientId);
        return ResponseEntity.ok(Map.of(
                "message", existingEntity != null ? "Client restored and registered successfully" : "Client registered successfully",
                "statusCode", 200,
                "entity", Map.of(
                        "clientId", clientId,
                        "clientSecret", rawSecret,
                        "authUrl", "http://localhost:8081/oauth2/authorize",
                        "tokenUrl", "http://localhost:8081/oauth2/token",
                        "userInfoUrl", "http://localhost:8081/userinfo",
                        "scopes", scopes,
                        "grantTypes", grantTypes,
                        "note", "Save the clientSecret now. It cannot be retrieved again."
                )
        ));
    }

    @GetMapping
    public ResponseEntity<?> listClients(@RequestParam(value = "includeDeleted", defaultValue = "false") boolean includeDeleted) {
        List<Map<String, Object>> clients = (includeDeleted
                ? registeredClientEntityRepository.findAll()
                : registeredClientEntityRepository.findByDeletedFalseOrderByClientIdAsc())
                .stream()
                .sorted((first, second) -> clean(first.getClientId()).compareToIgnoreCase(clean(second.getClientId())))
                .map(this::toClientResponse)
                .toList();

        return ResponseEntity.ok(Map.of(
                "message", "OAuth clients fetched successfully",
                "statusCode", 200,
                "entity", clients
        ));
    }

    @GetMapping("/{clientId}")
    public ResponseEntity<?> getClient(
            @PathVariable String clientId,
            @RequestParam(value = "includeDeleted", defaultValue = "false") boolean includeDeleted
    ) {
        RegisteredClientEntity entity = registeredClientEntityRepository.findByClientId(clientId).orElse(null);
        if (entity == null || (!includeDeleted && entity.isDeleted())) {
            return notFound("Client not found: " + clientId);
        }

        return ResponseEntity.ok(Map.of(
                "message", "OAuth client fetched successfully",
                "statusCode", 200,
                "entity", toClientResponse(entity)
        ));
    }

    @GetMapping("/registeredClients")
    public ResponseEntity<?> clientMetadata() {
        return ResponseEntity.ok(Map.of(
                "message", "OAuth client endpoints fetched successfully",
                "statusCode", 200,
                "entity", endpointMetadata()
        ));
    }

    @PutMapping("/{clientId}")
    public ResponseEntity<?> updateClient(
            @PathVariable String clientId,
            @RequestBody ClientRegistrationRequest req,
            @RequestParam(value = "modifiedBy", required = false) String modifiedBy
    ) {
        RegisteredClientEntity existing = registeredClientEntityRepository.findByClientId(clientId).orElse(null);
        if (existing == null || existing.isDeleted()) {
            return notFound("Client not found or deleted: " + clientId);
        }

        String updatedClientId = clean(req.getClientId()).isBlank() ? existing.getClientId() : clean(req.getClientId());
        RegisteredClientEntity duplicate = registeredClientEntityRepository.findByClientId(updatedClientId).orElse(null);
        if (duplicate != null && !duplicate.getId().equals(existing.getId()) && !duplicate.isDeleted()) {
            return badRequest("Client ID already exists: " + updatedClientId);
        }

        req.setClientId(updatedClientId);
        if (clean(req.getClientName()).isBlank()) req.setClientName(existing.getClientName());
        if (req.getRedirectUris() == null || req.getRedirectUris().isEmpty()) req.setRedirectUris(split(existing.getRedirectUris()));

        List<String> scopes = req.getScopes() == null || req.getScopes().isEmpty()
                ? split(existing.getScopes())
                : normalizeScopes(req.getScopes());
        List<String> grantTypes = req.getGrantTypes() == null || req.getGrantTypes().isEmpty()
                ? split(existing.getAuthorizationGrantTypes())
                : normalizeGrantTypes(req.getGrantTypes());

        String encodedSecret = clean(req.getClientSecret()).isBlank()
                ? existing.getClientSecret()
                : "{bcrypt}" + passwordEncoder.encode(req.getClientSecret());

        registeredClientRepository.save(buildRegisteredClient(existing.getId(), req, encodedSecret, scopes, grantTypes));
        RegisteredClientEntity saved = registeredClientEntityRepository.findById(existing.getId()).orElseThrow();
        saved.setModifiedBy(clean(modifiedBy).isBlank() ? "system" : modifiedBy);
        saved.setModifiedTime(Instant.now());
        registeredClientEntityRepository.save(saved);

        return ResponseEntity.ok(Map.of(
                "message", "OAuth client updated successfully",
                "statusCode", 200,
                "entity", toClientResponse(saved)
        ));
    }

    @DeleteMapping("/{clientId}")
    public ResponseEntity<?> deleteClient(
            @PathVariable String clientId,
            @RequestParam(value = "deletedBy", required = false) String deletedBy
    ) {
        RegisteredClientEntity existing = registeredClientEntityRepository.findByClientId(clientId).orElse(null);
        if (existing == null) {
            return notFound("Client not found: " + clientId);
        }
        if (existing.isDeleted()) {
            return badRequest("Client is already deleted: " + clientId);
        }

        existing.setDeleted(true);
        existing.setDeletedBy(clean(deletedBy).isBlank() ? "system" : deletedBy);
        existing.setDeletedTime(Instant.now());
        registeredClientEntityRepository.save(existing);

        log.warn("[OAuth2 Client] Soft deleted client: {}", clientId);
        return ResponseEntity.ok(Map.of(
                "message", "OAuth client deleted successfully",
                "statusCode", 200,
                "entity", toClientResponse(existing)
        ));
    }

    private RegisteredClient buildRegisteredClient(
            String id,
            ClientRegistrationRequest req,
            String encodedSecret,
            List<String> scopes,
            List<String> grantTypes
    ) {
        RegisteredClient.Builder builder = RegisteredClient.withId(id)
                .clientId(req.getClientId())
                .clientSecret(encodedSecret)
                .clientName(clean(req.getClientName()).isBlank() ? req.getClientId() : req.getClientName())
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
                .clientSettings(ClientSettings.builder()
                        .requireAuthorizationConsent(req.isRequireConsent())
                        .build())
                .tokenSettings(TokenSettings.builder()
                        .accessTokenTimeToLive(Duration.ofMinutes(Math.max(req.getAccessTokenTtlMinutes(), 1)))
                        .refreshTokenTimeToLive(Duration.ofHours(Math.max(req.getRefreshTokenTtlHours(), 1)))
                        .reuseRefreshTokens(true)
                        .build());

        if (req.getRedirectUris() != null) {
            req.getRedirectUris().stream().map(this::clean).filter(uri -> !uri.isBlank()).forEach(builder::redirectUri);
        }

        scopes.forEach(builder::scope);
        grantTypes.forEach(grantType -> {
            switch (grantType.toLowerCase()) {
                case "authorization_code" -> builder.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE);
                case "refresh_token" -> builder.authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN);
                case "client_credentials" -> builder.authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS);
                default -> log.warn("[OAuth2 Client] Ignoring unsupported grant type: {}", grantType);
            }
        });

        return builder.build();
    }

    private List<String> normalizeScopes(List<String> requestedScopes) {
        List<String> scopes = requestedScopes == null || requestedScopes.isEmpty()
                ? List.of(OidcScopes.OPENID, OidcScopes.PROFILE, OidcScopes.EMAIL)
                : requestedScopes;
        return scopes.stream().map(this::clean).filter(scope -> !scope.isBlank()).distinct().toList();
    }

    private List<String> normalizeGrantTypes(List<String> requestedGrantTypes) {
        List<String> grantTypes = requestedGrantTypes == null || requestedGrantTypes.isEmpty()
                ? List.of("authorization_code", "refresh_token")
                : requestedGrantTypes;
        return grantTypes.stream().map(this::clean).filter(grantType -> !grantType.isBlank()).distinct().toList();
    }

    private Map<String, Object> toClientResponse(RegisteredClientEntity entity) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", entity.getId());
        response.put("clientId", entity.getClientId());
        response.put("clientName", entity.getClientName());
        response.put("clientAuthenticationMethods", split(entity.getClientAuthenticationMethods()));
        response.put("grantTypes", split(entity.getAuthorizationGrantTypes()));
        response.put("redirectUris", split(entity.getRedirectUris()));
        response.put("scopes", split(entity.getScopes()));
        response.put("deleted", entity.isDeleted());
        response.put("modifiedBy", entity.getModifiedBy());
        response.put("modifiedTime", entity.getModifiedTime());
        response.put("deletedBy", entity.getDeletedBy());
        response.put("deletedTime", entity.getDeletedTime());
        response.putAll(endpointMetadata());
        return response;
    }

    private Map<String, Object> endpointMetadata() {
        Map<String, Object> endpoints = new LinkedHashMap<>();
        endpoints.put("authUrl", "http://localhost:8081/oauth2/authorize");
        endpoints.put("tokenUrl", "http://localhost:8081/oauth2/token");
        endpoints.put("userInfoUrl", "http://localhost:8081/userinfo");
        endpoints.put("jwksUrl", "http://localhost:8081/oauth2/jwks");
        endpoints.put("discoveryUrl", "http://localhost:8081/.well-known/openid-configuration");
        return endpoints;
    }

    private List<String> split(String value) {
        if (value == null || value.isBlank()) return new ArrayList<>();
        return Arrays.stream(value.split(","))
                .map(this::clean)
                .filter(item -> !item.isBlank())
                .toList();
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private ResponseEntity<?> badRequest(String message) {
        return ResponseEntity.badRequest().body(Map.of("message", message, "statusCode", 400));
    }

    private ResponseEntity<?> notFound(String message) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", message, "statusCode", 404));
    }

    private String generateSecret() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
