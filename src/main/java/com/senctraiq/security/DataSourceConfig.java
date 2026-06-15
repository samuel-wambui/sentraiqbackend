package com.senctraiq.security;

import com.zaxxer.hikari.HikariDataSource;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataSourceConfig {

    @Autowired
    private HikariDataSource hikariDataSource;

    @PreDestroy
    public void closeDataSource() {
        if (hikariDataSource != null) {
            hikariDataSource.close();
        }
    }
}
