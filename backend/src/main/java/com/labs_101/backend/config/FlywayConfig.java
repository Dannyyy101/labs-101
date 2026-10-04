package com.labs_101.backend.config;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.flyway.autoconfigure.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.persistence.EntityManagerFactory;

/**
 * Tables are still created by Hibernate ({@code ddl-auto=update}), the
 * migrations in {@code db/migration} only add what Hibernate can't (extensions,
 * trigram indexes). So they have to run after Hibernate instead of before it,
 * which is the Spring Boot default.
 */
@Configuration
public class FlywayConfig {

    // skip the migration Spring Boot runs before the EntityManagerFactory
    @Bean
    FlywayMigrationStrategy flywayMigrationStrategy() {
        return (flyway) -> {
        };
    }

    // depends on the EntityManagerFactory, so Hibernate has created the tables
    @Bean
    InitializingBean flywayMigrateAfterHibernate(Flyway flyway, EntityManagerFactory entityManagerFactory) {
        return flyway::migrate;
    }
}
