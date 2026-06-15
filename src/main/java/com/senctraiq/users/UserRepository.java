package com.senctraiq.users;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> { Optional<User> findByUsernameAndDeleted(String username, boolean deleted);

    Optional<User>  findByEmailAndDeleted(String email, boolean deleted);
    Optional<List<User>> findByDeletedAndIsOnShift(boolean deleted, boolean isOnShift);

    Optional<User> findByUsernameAndDeletedAndIsOnShift(String username, boolean deleted, boolean isOnShift);

    long countByDeletedFalseAndIsOnShiftTrue();

    @Query(value = """
    SELECT DISTINCT u.*
    FROM users u
    JOIN user_roles ur
        ON u.id = ur.user_id
    JOIN roles r
        ON r.id = ur.role_id
    WHERE r.name = :roleName
    """,
            nativeQuery = true)
    List<User> findUsersByRole(
            @Param("roleName") String roleName
    );
}
