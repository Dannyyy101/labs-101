package com.labs_101.backend;

import java.io.IOException;

import javax.sql.DataSource;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.testcontainers.utility.DockerImageName;

import com.labs_101.backend.entities.User;
import com.labs_101.backend.entities.food.Food;
import com.labs_101.backend.entities.food.TrackedFood;
import com.labs_101.backend.repositories.FoodRepository;
import com.labs_101.backend.repositories.TrackedFoodRepository;
import com.labs_101.backend.repositories.UserRepository;
import com.opentable.db.postgres.embedded.EmbeddedPostgres;

@Configuration
@EnableJpaRepositories(basePackageClasses = { FoodRepository.class, TrackedFoodRepository.class, UserRepository.class })
@EntityScan(basePackageClasses = { Food.class, TrackedFood.class, User.class })
public class EmbeddedPostgresConfiguration {
    private static EmbeddedPostgres embeddedPostgres;

    @Bean
    public DataSource dataSource() throws IOException {
        embeddedPostgres = EmbeddedPostgres.builder()
                .setImage(DockerImageName.parse("postgres:14.1"))
                .start();

        return embeddedPostgres.getPostgresDatabase();
    }

    public static class EmbeddedPostgresExtension implements AfterAllCallback {
        @Override
        public void afterAll(ExtensionContext context) throws Exception {
            if (embeddedPostgres == null) {
                return;
            }
            embeddedPostgres.close();
        }
    }
}