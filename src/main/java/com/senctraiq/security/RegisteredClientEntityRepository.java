package com.senctraiq.security;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RegisteredClientEntityRepository extends JpaRepository<RegisteredClientEntity, String> {
    Optional<RegisteredClientEntity> findByClientId(String clientId);
    Optional<RegisteredClientEntity> findByClientIdAndDeletedFalse(String clientId);
    Optional<RegisteredClientEntity> findByIdAndDeletedFalse(String id);
    List<RegisteredClientEntity> findByDeletedFalseOrderByClientIdAsc();
}
