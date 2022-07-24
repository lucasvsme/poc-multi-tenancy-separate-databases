package com.example.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

public final class TenantsContextInitializer implements ApplicationContextInitializer<GenericApplicationContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(TenantsContextInitializer.class);

    @Override
    public void initialize(GenericApplicationContext applicationContext) {
        final var environment = applicationContext.getEnvironment();

        for (final var tenantName : environment.getRequiredProperty("datasource.tenants.names", List.class)) {
            final var tenant = tenantName.toString();

            final var propertyFilesLocation = environment.getRequiredProperty("datasource.tenants.location");
            final var propertiesFile = Path.of(propertyFilesLocation, tenant + ".properties");

            try {
                final Properties properties = PropertiesLoaderUtils.loadAllProperties(propertiesFile.toString());
                requireProperty(properties, "datasource.tenant." + tenant + ".url");
                requireProperty(properties, "datasource.tenant." + tenant + ".username");
                requireProperty(properties, "datasource.tenant." + tenant + ".password");

                environment.getPropertySources()
                        .addLast(new PropertiesPropertySource(tenant, properties));

                LOGGER.info("Properties file loaded successfully (tenant={}, properties={})", tenant, properties);
            } catch (IOException exception) {
                throw new RuntimeException(exception);
            }
        }
    }

    private void requireProperty(Properties properties, String name) throws IllegalStateException {
        if (!properties.containsKey(name)) {
            throw new IllegalStateException("Property '" + name + "' is required");
        }
    }
}
