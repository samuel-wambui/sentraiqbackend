package com.senctraiq.security;


import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.proc.SecurityContext;
import com.senctraiq.users.User;
import com.senctraiq.users.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.authorization.*;
import org.springframework.security.oauth2.server.authorization.client.JdbcRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Slf4j
@Configuration
public class OAuth2AuthorizationServerConfig {

    private final UserRepository userRepository;
    private final OidcUserInfoMapper oidcUserInfoMapper;

    public OAuth2AuthorizationServerConfig(UserRepository userRepository, OidcUserInfoMapper oidcUserInfoMapper) {
        this.userRepository = userRepository;
        this.oidcUserInfoMapper = oidcUserInfoMapper;
    }

    @Bean
    @Primary
    public RegisteredClientRepository registeredClientRepository(RegisteredClientEntityRepository entityRepository) {
        JpaRegisteredClientRepositoryImpl repository = new JpaRegisteredClientRepositoryImpl(entityRepository);

        // Only save if not already in DB — prevents duplicate on restart
        if (repository.findByClientId("grafana") == null) {
            repository.save(RegisteredClient.withId(UUID.randomUUID().toString())
                    .clientId("grafana")
                    .clientSecret("{noop}secret")
                    .clientAuthenticationMethod(org.springframework.security.oauth2.core.ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                    .clientAuthenticationMethod(org.springframework.security.oauth2.core.ClientAuthenticationMethod.CLIENT_SECRET_POST)
                    .redirectUri("http://localhost:3000/login/generic_oauth")
                    .scope(OidcScopes.OPENID)
                    .scope(OidcScopes.PROFILE)
                    .scope(OidcScopes.EMAIL)
                    .authorizationGrantType(org.springframework.security.oauth2.core.AuthorizationGrantType.AUTHORIZATION_CODE)
                    .authorizationGrantType(org.springframework.security.oauth2.core.AuthorizationGrantType.REFRESH_TOKEN)
                    .clientSettings(ClientSettings.builder().requireAuthorizationConsent(false).build())
                    .build());
        }
        return repository;
    }

    @Bean
    public OAuth2AuthorizationService authorizationService(JdbcTemplate jdbcTemplate,
                                                           RegisteredClientRepository registeredClientRepository) {
        return new JdbcOAuth2AuthorizationService(jdbcTemplate, registeredClientRepository);
    }

    @Bean
    public OAuth2AuthorizationConsentService authorizationConsentService(JdbcTemplate jdbcTemplate,
                                                                          RegisteredClientRepository registeredClientRepository) {
        return new JdbcOAuth2AuthorizationConsentService(jdbcTemplate, registeredClientRepository);
    }

    @Bean
    public AuthorizationServerSettings authorizationServerSettings() {
        return AuthorizationServerSettings.builder()
                .issuer("http://localhost:8081")
                .build();
    }

    /**
     * Loads RSA key pair from DB if exists, otherwise generates and saves it.
     * This ensures the same key is used across restarts — required for JDBC token store.
     */
    @Bean
    public KeyPair rsaKeyPair(JdbcTemplate jdbcTemplate) {
        try {
            List<String[]> rows = jdbcTemplate.query(
                    "SELECT private_key, public_key FROM oauth2_rsa_key WHERE id = 'main'",
                    (rs, i) -> new String[]{rs.getString("private_key"), rs.getString("public_key")}
            );

            if (!rows.isEmpty()) {
                // Load existing key from DB
                String[] keys = rows.get(0);
                byte[] privateBytes = Base64.getDecoder().decode(keys[0]);
                byte[] publicBytes = Base64.getDecoder().decode(keys[1]);
                KeyFactory kf = KeyFactory.getInstance("RSA");
                RSAPrivateKey privateKey = (RSAPrivateKey) kf.generatePrivate(new PKCS8EncodedKeySpec(privateBytes));
                RSAPublicKey publicKey = (RSAPublicKey) kf.generatePublic(new X509EncodedKeySpec(publicBytes));
                return new KeyPair(publicKey, privateKey);
            }

            // Generate new key pair and persist it
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            KeyPair keyPair = generator.generateKeyPair();

            String privateEncoded = Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded());
            String publicEncoded = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());

            jdbcTemplate.update(
                    "INSERT INTO oauth2_rsa_key (id, private_key, public_key) VALUES ('main', ?, ?)",
                    privateEncoded, publicEncoded
            );

            return keyPair;
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to load or generate RSA key pair", ex);
        }
    }

    @Bean
    public RSAKey rsaJwk(KeyPair rsaKeyPair) {
        return new RSAKey.Builder((RSAPublicKey) rsaKeyPair.getPublic())
                .privateKey((RSAPrivateKey) rsaKeyPair.getPrivate())
                .keyID("Senctra-main-key")
                .build();
    }

    @Bean
    public com.nimbusds.jose.jwk.source.JWKSource<SecurityContext> jwkSource(RSAKey rsaJwk) {
        JWKSet jwkSet = new JWKSet(rsaJwk);
        return (selector, context) -> selector.select(jwkSet);
    }

    @Bean
    public JwtDecoder jwtDecoder(RSAKey rsaJwk) throws JOSEException {
        return NimbusJwtDecoder.withPublicKey(rsaJwk.toRSAPublicKey()).build();
    }

    @Bean
    public org.springframework.security.oauth2.jwt.JwtEncoder jwtEncoder(
            com.nimbusds.jose.jwk.source.JWKSource<SecurityContext> jwkSource) {
        return new NimbusJwtEncoder(jwkSource);
    }

    @Bean
    public OAuth2TokenCustomizer<JwtEncodingContext> tokenCustomizer() {
        return context -> {
            if (context.getTokenType().equals(OAuth2TokenType.ACCESS_TOKEN)
                    || "id_token".equals(context.getTokenType().getValue())) {

                String username = context.getPrincipal().getName();
                log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                log.info("[SSO TOKEN] Customizing {} for user: {}", context.getTokenType().getValue(), username);

                User user = userRepository.findByUsernameAndDeleted(username, false)
                        .orElseThrow(() -> new IllegalStateException("User not found: " + username));

                List<String> roles = new java.util.ArrayList<>(user.getRole().stream()
                        .map(role -> role.getName())
                        .toList());

                context.getClaims().claim("userId", String.valueOf(user.getId()));
                context.getClaims().claim("username", user.getUsername());
                context.getClaims().claim("roles", roles);
                context.getClaims().claim("grafana_roles", mapGrafanaRoles(roles));

                log.info("[SSO TOKEN] Claims added — userId: {}, roles: {}, grafana_roles: {}",
                        user.getId(), roles, mapGrafanaRoles(roles));
                log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

                if (context.getTokenType().equals(OAuth2TokenType.ACCESS_TOKEN)) {
                    context.getClaims().claim("scope", context.getAuthorizedScopes());
                }
            }
        };
    }

    private List<String> mapGrafanaRoles(List<String> internalRoles) {
        boolean isSuperUser = internalRoles.stream().anyMatch(r ->
                "SUPERUSER".equalsIgnoreCase(r)
                        || "SUPER_USER".equalsIgnoreCase(r)
                        || "ROLE_SUPERUSER".equalsIgnoreCase(r)
                        || "ROLE_SUPER_USER".equalsIgnoreCase(r));

        if (isSuperUser) return new java.util.ArrayList<>(java.util.Arrays.asList("Admin", "Editor", "Viewer"));
        return new java.util.ArrayList<>(java.util.Arrays.asList("Viewer"));
    }
}
