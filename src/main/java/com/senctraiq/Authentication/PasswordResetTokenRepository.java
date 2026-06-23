package com.senctraiq.Authentication;

import com.senctraiq.users.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findByTokenHashAndPurposeAndUsedAtIsNull(String tokenHash, String purpose);

    List<PasswordResetToken> findByUserAndPurposeAndUsedAtIsNull(User user, String purpose);
}
