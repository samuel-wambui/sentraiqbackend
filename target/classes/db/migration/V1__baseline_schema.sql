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

-- Idempotent migrations for existing databases. CREATE TABLE IF NOT EXISTS does
-- not add columns when Spring Authorization Server upgrades its JDBC schema.
ALTER TABLE oauth2_registered_client
  ADD COLUMN IF NOT EXISTS post_logout_redirect_uris TEXT,
  ADD COLUMN IF NOT EXISTS modified_by VARCHAR(200),
  ADD COLUMN IF NOT EXISTS modified_time TIMESTAMP,
  ADD COLUMN IF NOT EXISTS deleted_by VARCHAR(200),
  ADD COLUMN IF NOT EXISTS deleted_time TIMESTAMP,
  ADD COLUMN IF NOT EXISTS deleted BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE IF NOT EXISTS oauth2_authorization (
  id VARCHAR(100) PRIMARY KEY,
  registered_client_id VARCHAR(100) NOT NULL,
  principal_name VARCHAR(200) NOT NULL,
  authorization_grant_type VARCHAR(100) NOT NULL,
  attributes TEXT,
  state VARCHAR(500),
  authorization_code_value TEXT,
  authorization_code_issued_at TIMESTAMP,
  authorization_code_expires_at TIMESTAMP,
  authorization_code_metadata TEXT,
  access_token_value TEXT,
  access_token_issued_at TIMESTAMP,
  access_token_expires_at TIMESTAMP,
  access_token_metadata TEXT,
  access_token_type VARCHAR(100),
  access_token_scopes TEXT,
  authorized_scopes VARCHAR(1000),
  refresh_token_value TEXT,
  refresh_token_issued_at TIMESTAMP,
  refresh_token_expires_at TIMESTAMP,
  refresh_token_metadata TEXT,
  oidc_id_token_value TEXT,
  oidc_id_token_issued_at TIMESTAMP,
  oidc_id_token_expires_at TIMESTAMP,
  oidc_id_token_metadata TEXT,
  user_code_value TEXT,
  user_code_issued_at TIMESTAMP,
  user_code_expires_at TIMESTAMP,
  user_code_metadata TEXT,
  device_code_value TEXT,
  device_code_issued_at TIMESTAMP,
  device_code_expires_at TIMESTAMP,
  device_code_metadata TEXT
);

ALTER TABLE oauth2_authorization
  ADD COLUMN IF NOT EXISTS authorized_scopes VARCHAR(1000),
  ADD COLUMN IF NOT EXISTS user_code_value TEXT,
  ADD COLUMN IF NOT EXISTS user_code_issued_at TIMESTAMP,
  ADD COLUMN IF NOT EXISTS user_code_expires_at TIMESTAMP,
  ADD COLUMN IF NOT EXISTS user_code_metadata TEXT,
  ADD COLUMN IF NOT EXISTS device_code_value TEXT,
  ADD COLUMN IF NOT EXISTS device_code_issued_at TIMESTAMP,
  ADD COLUMN IF NOT EXISTS device_code_expires_at TIMESTAMP,
  ADD COLUMN IF NOT EXISTS device_code_metadata TEXT;

-- Spring Authorization Server binds PostgreSQL token/code values as text.
-- The official schema says to convert every blob column to text for PostgreSQL.
ALTER TABLE oauth2_authorization
  ALTER COLUMN authorization_code_value TYPE TEXT USING authorization_code_value::TEXT,
  ALTER COLUMN access_token_value TYPE TEXT USING access_token_value::TEXT,
  ALTER COLUMN refresh_token_value TYPE TEXT USING refresh_token_value::TEXT,
  ALTER COLUMN oidc_id_token_value TYPE TEXT USING oidc_id_token_value::TEXT,
  ALTER COLUMN user_code_value TYPE TEXT USING user_code_value::TEXT,
  ALTER COLUMN device_code_value TYPE TEXT USING device_code_value::TEXT;

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
