package com.example.testing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.env.MapPropertySource;
import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.ContextCustomizerFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.List;
import java.util.Map;

public final class TenantDatabaseFactory implements ContextCustomizerFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(TenantDatabaseFactory.class);

    @Override
    public ContextCustomizer createContextCustomizer(Class<?> testClass,
                                                     List<ContextConfigurationAttributes> configAttributes) {
        if (!AnnotatedElementUtils.hasAnnotation(testClass, TenantDatabases.class)) {
            return null;
        }

        final var tenants = testClass.getAnnotationsByType(TenantDatabase.class);
        LOGGER.info("Provisioning tenant databases (tenants={})", (Object) tenants);

        return (configurableApplicationContext, mergedContextConfiguration) -> {
            for (final var tenant : tenants) {
                final var container = new PostgreSQLContainer<>(DockerImageName.parse("postgres"));
                container.start();

                final var name = tenant.name();
                final var propertySource = new MapPropertySource(name, Map.ofEntries(
                        Map.entry("datasource.tenant." + name + ".url", container.getJdbcUrl()),
                        Map.entry("datasource.tenant." + name + ".username", container.getUsername()),
                        Map.entry("datasource.tenant." + name + ".password", container.getPassword())
                ));

                configurableApplicationContext.getEnvironment()
                        .getPropertySources()
                        .addFirst(propertySource);

                LOGGER.info("Database tenant provisioned (tenant={}, properties={})", name, propertySource.getSource());
            }
        };
    }
}
