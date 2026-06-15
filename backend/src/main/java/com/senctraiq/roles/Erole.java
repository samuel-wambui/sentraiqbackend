package com.senctraiq.roles;


import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.senctraiq.roles.Permissions.*;


@RequiredArgsConstructor
public enum Erole {
    USER(Collections.emptySet()),
    ROLE_SUPERUSER(Set.of(
            SUPERUSER_READ,
            SUPERUSER_CREATE,
            SUPERUSER_UPDATE,
            SUPERUSER_DELETE,
            ADMIN_READ,
            ADMIN_UPDATE,
            ADMIN_CREATE,
            ADMIN_DELETE,
            ANALYST_READ,
            ANALYST_UPDATE,
            ANALYST_CREATE,
            ANALYST_DELETE

    )),
    ADMIN(Set.of(
            ADMIN_READ,
            ADMIN_UPDATE,
            ADMIN_CREATE,
            ADMIN_DELETE,
            ANALYST_READ,
            ANALYST_UPDATE,
            ANALYST_CREATE,
            ANALYST_DELETE
    )),
    ANALYST(Set.of(
            ANALYST_READ,
            ANALYST_UPDATE,
            ANALYST_CREATE,
            ANALYST_DELETE

    )),
    ROLE_SUPERVISOR(Collections.emptySet()),

    ROLE_WAITER(Collections.emptySet()),

    ROLE_PARTNER(Set.of(
            ANALYST_READ,
            ANALYST_UPDATE,
            ANALYST_CREATE,
            ANALYST_DELETE)),

    ROLE_SYSTEM(Set.of(
            ADMIN_UPDATE,
            ADMIN_CREATE,
            ADMIN_READ,
            ADMIN_DELETE,
            ANALYST_READ,
            ANALYST_UPDATE,
            ANALYST_CREATE,
            ANALYST_DELETE));
    @Getter
    private final Set<Permissions> permissions;

    public List<SimpleGrantedAuthority> getAuthorities() {
        List<SimpleGrantedAuthority> authorities = getPermissions().stream()
                .map(permission -> new SimpleGrantedAuthority(permission.name()))
                .collect(Collectors.toList());
        authorities.add(new SimpleGrantedAuthority(this.name()));
        return authorities;
    }
}
