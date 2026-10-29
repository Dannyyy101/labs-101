package com.labs_101.backend.config;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Field;
import java.lang.reflect.RecordComponent;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.labs_101.backend.security.CurrentUser;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.JsonSchema;
import io.swagger.v3.oas.models.media.Schema;
import tools.jackson.databind.JsonNode;

/**
 * The OpenAPI spec at {@code /v3/api-docs} is the contract with the frontend and
 * the iOS app, both generate their clients from {@code api/openapi.json}
 * (written by {@code OpenApiSpecTest}).
 * <p>
 * Jackson always writes every property, so every property of a DTO the backend
 * answers with is required, the ones that can be {@code null} are marked with
 * JSpecify's {@link Nullable} and are nullable in the spec. DTOs that are only
 * sent to the backend may leave out the nullable ones.
 */
@Configuration
@OpenAPIDefinition(info = @Info(title = "labs-101", version = "1"), security = @SecurityRequirement(name = "zitadel"))
@SecurityScheme(name = "zitadel", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class OpenApiConfig {
    private static final String DTO_PACKAGE = "com.labs_101.backend.dtos";
    // Spring Data REST adds its converter, the controllers answer with plain JSON anyway
    private static final String HAL = "application/vnd.hal+json";
    private static final String JSON = "application/json";
    private static final String MULTIPART = "multipart/form-data";
    // Page and Slice of Spring Data as they are written, Pageable is the optional query parameter
    private static final Pattern SPRING_DATA = Pattern.compile("(Page|Slice)\\w+Dto|PageableObject|SortObject");

    static {
        // comes from the access token, not from the client
        SpringDocUtils.getConfig().addAnnotationsToIgnore(CurrentUser.class)
                // free JSON of HealthKit, not the methods of Jackson's tree
                .replaceWithSchema(JsonNode.class, typed("object"));
    }

    /**
     * Controller and method, e.g. {@code friendGetProfile}: the methods of the Swift client are named after it
     * and springdoc would number the ones with the same name in the order it finds them.
     */
    @Bean
    OperationCustomizer operationIds() {
        return (operation, handler) -> {
            String controller = handler.getBeanType().getSimpleName().replace("Controller", "");
            String method = handler.getMethod().getName();
            return operation.operationId(StringUtils.uncapitalize(controller) + StringUtils.capitalize(method));
        };
    }

    @Bean
    OpenApiCustomizer contractCustomizer() {
        Map<String, Class<?>> dtos = dtoClasses();
        return openApi -> {
            Set<String> answered = responseSchemas(openApi);
            openApi.getComponents().getSchemas().forEach((name, schema) -> {
                if (SPRING_DATA.matcher(name).matches()) {
                    schema.setRequired(List.copyOf(schema.getProperties().keySet()));
                    return;
                }
                Class<?> type = dtos.get(name);
                markRequired(schema, type, answered.contains(name));
                // a subtype of a sealed interface has its own properties next to the reference to the interface
                List<Schema> parts = schema.getAllOf() == null ? List.of() : schema.getAllOf();
                for (Schema part : parts) {
                    if (part.get$ref() == null) {
                        markRequired(part, type, answered.contains(name));
                    }
                }
                mapSubtypes(schema, type);
            });
            openApi.getPaths().values().forEach(path -> path.readOperations().forEach(operation -> {
                operation.getResponses().values().forEach(response -> replaceHal(response.getContent()));
                // springdoc leaves out required for @RequestPart, the Swift generator skips an optional multipart body
                if (operation.getRequestBody() != null && operation.getRequestBody().getContent().containsKey(MULTIPART)) {
                    operation.getRequestBody().setRequired(true);
                }
            }));
        };
    }

    private static void markRequired(Schema<?> schema, @Nullable Class<?> type, boolean answered) {
        if (type == null || schema.getProperties() == null) {
            return;
        }
        Map<String, Schema> properties = schema.getProperties();
        properties.replaceAll((property, propertySchema) -> {
            Optional<AnnotatedType> declared = declaredType(type, property);
            if (declared.isEmpty()) {
                return propertySchema;
            }
            boolean nullable = declared.get().isAnnotationPresent(Nullable.class);
            if (!nullable || answered) {
                schema.addRequiredItem(property);
            }
            return nullable ? nullable(propertySchema) : propertySchema;
        });
    }

    // the names Jackson writes into the discriminator, without them the clients would expect the names of the schemas
    private static void mapSubtypes(Schema<?> schema, @Nullable Class<?> type) {
        if (type == null || schema.getDiscriminator() == null || !type.isAnnotationPresent(JsonSubTypes.class)) {
            return;
        }
        for (JsonSubTypes.Type subtype : type.getAnnotation(JsonSubTypes.class).value()) {
            schema.getDiscriminator().mapping(subtype.name(), "#/components/schemas/" + subtype.value().getSimpleName());
        }
    }

    // names of the schemas in the responses, nested ones too
    private static Set<String> responseSchemas(OpenAPI openApi) {
        Map<String, Schema> schemas = openApi.getComponents().getSchemas();
        Set<String> names = new HashSet<>();
        Deque<Schema> open = new ArrayDeque<>();
        openApi.getPaths().values().forEach(path -> path.readOperations().forEach(operation -> operation
                .getResponses().values().stream()
                .filter(response -> response.getContent() != null)
                .forEach(response -> response.getContent().values().forEach(media -> open.add(media.getSchema())))));
        while (!open.isEmpty()) {
            Schema schema = open.pop();
            String ref = schema.get$ref();
            if (ref != null) {
                String name = ref.substring(ref.lastIndexOf('/') + 1);
                if (names.add(name) && schemas.containsKey(name)) {
                    open.add(schemas.get(name));
                }
                continue;
            }
            open.addAll(children(schema));
        }
        return names;
    }

    private static List<Schema> children(Schema<?> schema) {
        List<Schema> children = new ArrayList<>();
        if (schema.getProperties() != null) {
            children.addAll(schema.getProperties().values());
        }
        if (schema.getItems() != null) {
            children.add(schema.getItems());
        }
        if (schema.getAdditionalProperties() instanceof Schema<?> additional) {
            children.add(additional);
        }
        Stream.of(schema.getOneOf(), schema.getAnyOf(), schema.getAllOf())
                .filter(Objects::nonNull)
                .forEach(children::addAll);
        return children;
    }

    // a reference can't get "null" as second type, it is wrapped in allOf (the Swift generator can't read oneOf with null)
    private static Schema<?> nullable(Schema<?> schema) {
        if (schema.get$ref() != null) {
            Schema<?> wrapped = new JsonSchema().allOf(List.of(new JsonSchema().$ref(schema.get$ref())));
            wrapped.addType("object");
            wrapped.addType("null");
            return wrapped;
        }
        schema.addType("null");
        if (schema.getEnum() != null) {
            schema.getEnum().add(null);
        }
        return schema;
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

    // by simple name like the schemas, nested ones like FoodWithLastEntryAndPortionsDto.LastEntry and
    // the sealed interfaces of subtypes too
    private static Map<String, Class<?>> dtoClasses() {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false) {
            @Override
            protected boolean isCandidateComponent(AnnotatedBeanDefinition definition) {
                return definition.getMetadata().isIndependent();
            }
        };
        scanner.addIncludeFilter((reader, factory) -> true);
        Map<String, Class<?>> dtos = new HashMap<>();
        scanner.findCandidateComponents(DTO_PACKAGE).forEach(candidate -> {
            Class<?> type = ClassUtils.resolveClassName(candidate.getBeanClassName(), OpenApiConfig.class.getClassLoader());
            dtos.put(type.getSimpleName(), type);
        });
        return dtos;
    }
}
