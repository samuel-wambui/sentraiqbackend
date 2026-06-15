package com.senctraiq.security;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;

import java.util.Arrays;
import java.util.stream.Collectors;

@Repository
public class JpaRegisteredClientRepositoryImpl implements RegisteredClientRepository {

    private final RegisteredClientEntityRepository repo;

    public JpaRegisteredClientRepositoryImpl(RegisteredClientEntityRepository repo) {
        this.repo = repo;
    }

    @Override
    @Transactional
    public void save(RegisteredClient registeredClient) {
        RegisteredClientEntity entity = toEntity(registeredClient);
        repo.save(entity);
    }

    @Override
    public RegisteredClient findById(String id) {
        return repo.findById(id).map(this::toRegisteredClient).orElse(null);
    }

    @Override
    public RegisteredClient findByClientId(String clientId) {
        return repo.findByClientId(clientId).map(this::toRegisteredClient).orElse(null);
    }

    private RegisteredClient toRegisteredClient(RegisteredClientEntity e) {
        RegisteredClient.Builder builder = RegisteredClient.withId(e.getId())
                .clientId(e.getClientId())
                .clientSecret(e.getClientSecret());

        if (e.getClientIdIssuedAt() != null) builder.clientIdIssuedAt(e.getClientIdIssuedAt());
        if (e.getClientSecretExpiresAt() != null) builder.clientSecretExpiresAt(e.getClientSecretExpiresAt());
        if (e.getClientName() != null) builder.clientName(e.getClientName());

        if (e.getClientAuthenticationMethods() != null && !e.getClientAuthenticationMethods().isEmpty()) {
            Arrays.stream(e.getClientAuthenticationMethods().split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .forEach(s -> builder.clientAuthenticationMethod(new ClientAuthenticationMethod(s)));
        }

        if (e.getAuthorizationGrantTypes() != null && !e.getAuthorizationGrantTypes().isEmpty()) {
            Arrays.stream(e.getAuthorizationGrantTypes().split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .forEach(s -> builder.authorizationGrantType(new AuthorizationGrantType(s)));
        }

        if (e.getRedirectUris() != null && !e.getRedirectUris().isEmpty()) {
            Arrays.stream(e.getRedirectUris().split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .forEach(builder::redirectUri);
        }

        if (e.getScopes() != null && !e.getScopes().isEmpty()) {
            Arrays.stream(e.getScopes().split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .forEach(builder::scope);
        }

        builder.clientSettings(ClientSettings.builder()
                .requireAuthorizationConsent(false)
                .requireProofKey(false)
                .build());
        builder.tokenSettings(TokenSettings.builder().build());

        return builder.build();
    }

    private RegisteredClientEntity toEntity(RegisteredClient rc) {
        RegisteredClientEntity e = RegisteredClientEntity.builder()
                .id(rc.getId())
                .clientId(rc.getClientId())
                .clientSecret(rc.getClientSecret())
                .clientIdIssuedAt(rc.getClientIdIssuedAt())
                .clientSecretExpiresAt(rc.getClientSecretExpiresAt())
                .clientName(rc.getClientName())
                .clientAuthenticationMethods(rc.getClientAuthenticationMethods().stream().map(ClientAuthenticationMethod::getValue).collect(Collectors.joining(",")))
                .authorizationGrantTypes(rc.getAuthorizationGrantTypes().stream().map(AuthorizationGrantType::getValue).collect(Collectors.joining(",")))
                .redirectUris(String.join(",", rc.getRedirectUris()))
                .scopes(String.join(",", rc.getScopes()))
                .clientSettings("")
                .tokenSettings("")
                .build();

        return e;
    }
}
