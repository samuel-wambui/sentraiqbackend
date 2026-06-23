DO $$
BEGIN
  IF EXISTS (
    SELECT 1
    FROM information_schema.tables
    WHERE table_schema = 'public'
      AND table_name = 'users'
  ) THEN
    ALTER TABLE users
      ADD COLUMN IF NOT EXISTS force_password_change BOOLEAN NOT NULL DEFAULT FALSE;

    CREATE TABLE IF NOT EXISTS password_reset_tokens (
      id BIGSERIAL PRIMARY KEY,
      user_id BIGINT NOT NULL,
      token_hash VARCHAR(64) NOT NULL UNIQUE,
      purpose VARCHAR(40) NOT NULL,
      expires_at TIMESTAMP NOT NULL,
      used_at TIMESTAMP,
      created_at TIMESTAMP NOT NULL DEFAULT NOW(),
      CONSTRAINT fk_password_reset_tokens_user
        FOREIGN KEY (user_id) REFERENCES users(id)
    );

    CREATE UNIQUE INDEX IF NOT EXISTS idx_password_reset_token_hash
      ON password_reset_tokens(token_hash);

    CREATE INDEX IF NOT EXISTS idx_password_reset_token_user_purpose
      ON password_reset_tokens(user_id, purpose);
  END IF;
END $$;
