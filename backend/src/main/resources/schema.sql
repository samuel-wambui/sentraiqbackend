-- Schema required by Spring Authorization Server (PostgreSQL)
-- Creates tables used by JdbcRegisteredClientRepository, JdbcOAuth2AuthorizationService
-- and JdbcOAuth2AuthorizationConsentService

CREATE TABLE IF NOT EXISTS oauth2_registered_client (
  id VARCHAR(100) PRIMARY KEY,
  client_id VARCHAR(100) NOT NULL,
  client_id_issued_at TIMESTAMP,
  client_secret VARCHAR(200),
  client_secret_expires_at TIMESTAMP,
  client_name VARCHAR(200),
  client_authentication_methods TEXT,
  authorization_grant_types TEXT,
  redirect_uris TEXT,
  scopes TEXT,
  client_settings TEXT,
  token_settings TEXT
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_registered_client_client_id ON oauth2_registered_client (client_id);

CREATE TABLE IF NOT EXISTS oauth2_authorization (
  id VARCHAR(100) PRIMARY KEY,
  registered_client_id VARCHAR(100) NOT NULL,
  principal_name VARCHAR(200) NOT NULL,
  authorization_grant_type VARCHAR(100) NOT NULL,
  attributes TEXT,
  state VARCHAR(500),
  authorization_code_value BYTEA,
  authorization_code_issued_at TIMESTAMP,
  authorization_code_expires_at TIMESTAMP,
  authorization_code_metadata TEXT,
  access_token_value BYTEA,
  access_token_issued_at TIMESTAMP,
  access_token_expires_at TIMESTAMP,
  access_token_metadata TEXT,
  access_token_type VARCHAR(100),
  access_token_scopes TEXT,
  refresh_token_value BYTEA,
  refresh_token_issued_at TIMESTAMP,
  refresh_token_expires_at TIMESTAMP,
  refresh_token_metadata TEXT,
  oidc_id_token_value BYTEA,
  oidc_id_token_issued_at TIMESTAMP,
  oidc_id_token_expires_at TIMESTAMP,
  oidc_id_token_metadata TEXT
);

CREATE INDEX IF NOT EXISTS ix_auth_registered_client_id ON oauth2_authorization (registered_client_id);
CREATE INDEX IF NOT EXISTS ix_auth_principal_name ON oauth2_authorization (principal_name);

CREATE TABLE IF NOT EXISTS oauth2_authorization_consent (
  registered_client_id VARCHAR(100) NOT NULL,
  principal_name VARCHAR(200) NOT NULL,
  authorities TEXT,
  CONSTRAINT pk_oauth2_authorization_consent PRIMARY KEY (registered_client_id, principal_name)
);

-- Table used by the project to persist generated RSA key pair
CREATE TABLE IF NOT EXISTS oauth2_rsa_key (
  id VARCHAR(50) PRIMARY KEY,
  private_key TEXT,
  public_key TEXT
);

