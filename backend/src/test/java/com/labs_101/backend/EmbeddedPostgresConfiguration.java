package com.labs_101.backend;

import java.io.IOException;

import javax.sql.DataSource;

import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.testcontainers.utility.DockerImageName;

import com.labs_101.backend.config.FlywayConfig;
import com.labs_101.backend.entities.User;
import com.labs_101.backend.entities.food.Food;
import com.labs_101.backend.entities.food.TrackedFood;
import com.labs_101.backend.repositories.FoodRepository;
import com.labs_101.backend.repositories.TrackedFoodRepository;
import com.labs_101.backend.repositories.UserRepository;
import com.opentable.db.postgres.embedded.EmbeddedPostgres;

@Configuration
@Import(FlywayConfig.class)
@EnableJpaRepositories(basePackageClasses = { FoodRepository.class, TrackedFoodRepository.class, UserRepository.class })
@EntityScan(basePackageClasses = { Food.class, TrackedFood.class, User.class })
public class EmbeddedPostgresConfiguration {
    // lives as long as the (cached) Spring context, closing it after each test
    // class would break other test classes reusing the same context
    @Bean(destroyMethod = "close")
    public EmbeddedPostgres embeddedPostgres() throws IOException {
        return EmbeddedPostgres.builder()
                .setImage(DockerImageName.parse("postgres:14.1"))
                .start();
    }

    @Bean
    public DataSource dataSource(EmbeddedPostgres embeddedPostgres) {
        return embeddedPostgres.getPostgresDatabase();
    }
}