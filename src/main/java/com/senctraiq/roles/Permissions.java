package com.senctraiq.roles;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum Permissions {
    SUPERUSER_READ("superuser:read"),
    SUPERUSER_CREATE("superuser:create"),
    SUPERUSER_UPDATE("superuser:update"),
    SUPERUSER_DELETE("superuser:delete"),
    ADMIN_READ("admin:read"),
    ADMIN_CREATE("admin:create"),
    ADMIN_UPDATE("admin:update"),
    ADMIN_DELETE("admin:delete"),
    ANALYST_READ("analyst:read"),
    ANALYST_CREATE("analyst:create"),
    ANALYST_UPDATE("analyst:update"),
    ANALYST_DELETE("analyst:delete");


    public final String permission;
}
