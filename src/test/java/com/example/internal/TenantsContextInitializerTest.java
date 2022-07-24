package com.example.internal;

import org.junit.jupiter.api.Test;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.env.MapPropertySource;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TenantsContextInitializerTest {

    @Test
    void readingTenantPropertiesFromFile() {
        final var applicationContext = new GenericApplicationContext();
        applicationContext.getEnvironment()
                .getPropertySources()
                .addLast(new MapPropertySource("default", Map.ofEntries(
                        Map.entry("datasource.tenants.location", ""),
                        Map.entry("datasource.tenants.names", "tenant-a")
                )));

        new TenantsContextInitializer().initialize(applicationContext);

        final var environment = applicationContext.getEnvironment();
        assertEquals("jdbc:postgresql://localhost:5432/", environment.getRequiredProperty("datasource.tenant.tenant-a.url"));
        assertEquals("user", environment.getRequiredProperty("datasource.tenant.tenant-a.username"));
        assertEquals("password", environment.getRequiredProperty("datasource.tenant.tenant-a.password"));
    }

    @Test
    void tenantWithoutPropertiesFile() {
        final var applicationContext = new GenericApplicationContext();
        applicationContext.getEnvironment()
                .getPropertySources()
                .addLast(new MapPropertySource("default", Map.ofEntries(
                        Map.entry("datasource.tenants.location", ""),
                        Map.entry("datasource.tenants.names", "tenant-b")
                )));

        final var applicationContextInitializer = new TenantsContextInitializer();
        final var exception = assertThrows(
                IllegalStateException.class,
                () -> applicationContextInitializer.initialize(applicationContext)
        );

        assertEquals("Property 'datasource.tenant.tenant-b.url' is required", exception.getMessage());
        assertNull(exception.getCause());
    }
}
