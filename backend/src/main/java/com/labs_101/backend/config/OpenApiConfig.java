package com.labs_101.backend.config;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Field;
import java.lang.reflect.RecordComponent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import org.jspecify.annotations.Nullable;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.ClassUtils;

import com.labs_101.backend.security.CurrentUser;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.JsonSchema;
import io.swagger.v3.oas.models.media.Schema;
import tools.jackson.databind.JsonNode;

/**
 * The OpenAPI spec at {@code /v3/api-docs} is the contract with the frontend and
 * the iOS app, both generate their clients from {@code api/openapi.json}
 * (written by {@code OpenApiSpecTest}).
 * <p>
 * Jackson always writes every property, so every property of a DTO is required,
 * the ones that can be {@code null} are marked with JSpecify's {@link Nullable}
 * and are optional and nullable in the spec.
 */
@Configuration
@OpenAPIDefinition(info = @Info(title = "labs-101", version = "1"), security = @SecurityRequirement(name = "zitadel"))
@SecurityScheme(name = "zitadel", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class OpenApiConfig {
    private static final String DTO_PACKAGE = "com.labs_101.backend.dtos";
    // Spring Data REST adds its converter, the controllers answer with plain JSON anyway
    private static final String HAL = "application/vnd.hal+json";
    private static final String JSON = "application/json";
    // Page and Slice of Spring Data as they are written, Pageable is the optional query parameter
    private static final Pattern SPRING_DATA = Pattern.compile("(Page|Slice)\\w+Dto|PageableObject|SortObject");

    static {
        // comes from the access token, not from the client
        SpringDocUtils.getConfig().addAnnotationsToIgnore(CurrentUser.class)
                // free JSON of HealthKit, not the methods of Jackson's tree
                .replaceWithSchema(JsonNode.class, typed("object"));
    }

    @Bean
    OpenApiCustomizer contractCustomizer() {
        Map<String, Class<?>> dtos = dtoClasses();
        return openApi -> {
            openApi.getComponents().getSchemas().forEach((name, schema) -> {
                if (SPRING_DATA.matcher(name).matches()) {
                    schema.setRequired(List.copyOf(schema.getProperties().keySet()));
                    return;
                }
                markRequired(schema, dtos.get(name));
            });
            openApi.getPaths().values().forEach(path -> path.readOperations().forEach(operation -> operation
                    .getResponses().values().forEach(response -> replaceHal(response.getContent()))));
        };
    }

    private static void markRequired(Schema<?> schema, @Nullable Class<?> type) {
        if (type == null || schema.getProperties() == null) {
            return;
        }
        Map<String, Schema> properties = schema.getProperties();
        properties.replaceAll((property, propertySchema) -> {
            Optional<AnnotatedType> declared = declaredType(type, property);
            if (declared.isEmpty()) {
                return propertySchema;
            }
            if (declared.get().isAnnotationPresent(Nullable.class)) {
                return nullable(propertySchema);
            }
            schema.addRequiredItem(property);
            return propertySchema;
        });
    }

    // a reference can't get "null" as second type, it is wrapped in oneOf
    private static Schema<?> nullable(Schema<?> schema) {
        if (schema.get$ref() == null) {
            schema.addType("null");
            return schema;
        }
        return new JsonSchema().oneOf(List.of(new JsonSchema().$ref(schema.get$ref()), typed("null")));
    }

    private static Schema<?> typed(String type) {
        Schema<?> schema = new JsonSchema();
        schema.addType(type);
        return schema;
    }

    private static void replaceHal(@Nullable Content content) {
        if (content == null || !content.containsKey(HAL)) {
            return;
        }
        content.putIfAbsent(JSON, content.get(HAL));
        content.remove(HAL);
    }

    // record component or field of the property, the type carries the @Nullable
    private static Optional<AnnotatedType> declaredType(Class<?> type, String property) {
        if (type.isRecord()) {
            for (RecordComponent component : type.getRecordComponents()) {
                if (component.getName().equals(property)) {
                    return Optional.of(component.getAnnotatedType());
                }
            }
            return Optional.empty();
        }
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            for (Field field : current.getDeclaredFields()) {
                if (field.getName().equals(property)) {
                    return Optional.of(field.getAnnotatedType());
                }
            }
        }
        return Optional.empty();
    }

    // by simple name like the schemas, nested ones like FoodWithLastEntryAndPortionsDto.LastEntry too
    private static Map<String, Class<?>> dtoClasses() {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter((reader, factory) -> true);
        Map<String, Class<?>> dtos = new HashMap<>();
        scanner.findCandidateComponents(DTO_PACKAGE).forEach(candidate -> {
            Class<?> type = ClassUtils.resolveClassName(candidate.getBeanClassName(), OpenApiConfig.class.getClassLoader());
            dtos.put(type.getSimpleName(), type);
        });
        return dtos;
    }
}
