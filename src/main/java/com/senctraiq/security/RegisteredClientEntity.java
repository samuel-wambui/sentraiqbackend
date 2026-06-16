package com.senctraiq.security;

import lombok.*;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "oauth2_registered_client")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisteredClientEntity {
    @Id
    private String id;

    @Column(name = "client_id", nullable = false, unique = true)
    private String clientId;

    @Column(name = "client_id_issued_at")
    private Instant clientIdIssuedAt;

    @Column(name = "client_secret")
    private String clientSecret;

    @Column(name = "client_secret_expires_at")
    private Instant clientSecretExpiresAt;

    @Column(name = "client_name")
    private String clientName;

    @Column(name = "client_authentication_methods", columnDefinition = "text")
    private String clientAuthenticationMethods;

    @Column(name = "authorization_grant_types", columnDefinition = "text")
    private String authorizationGrantTypes;

    @Column(name = "redirect_uris", columnDefinition = "text")
    private String redirectUris;

    @Column(name = "scopes", columnDefinition = "text")
    private String scopes;

    @Column(name = "client_settings", columnDefinition = "text")
    private String clientSettings;

    @Column(name = "token_settings", columnDefinition = "text")
    private String tokenSettings;

    @Column(name = "modified_by")
    private String modifiedBy;

    @Column(name = "modified_time")
    private Instant modifiedTime;

    @Column(name = "deleted_by")
    private String deletedBy;

    @Column(name = "deleted_time")
    private Instant deletedTime;

    @Builder.Default
    @Column(name = "deleted", nullable = false)
    private boolean deleted = false;
}
