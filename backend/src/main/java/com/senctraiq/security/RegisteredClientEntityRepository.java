package com.senctraiq.security;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RegisteredClientEntityRepository extends JpaRepository<RegisteredClientEntity, String> {
    Optional<RegisteredClientEntity> findByClientId(String clientId);
}

