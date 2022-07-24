package com.example.internal;

import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;

import javax.sql.DataSource;
import java.util.Map;

@Configuration
public class TenantsDatabaseInitializer implements ApplicationListener<ContextRefreshedEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(TenantsDatabaseInitializer.class);

    @Override
    @SuppressWarnings("unchecked")
    public void onApplicationEvent(ContextRefreshedEvent event) {
        LOGGER.info("Initializing tenant databases");

        final var context = event.getApplicationContext();
        final var environment = context.getEnvironment();

        final var migrationsLocation = environment.getRequiredProperty("spring.flyway.locations");
        final var dataSources = (Map<Object, Object>) context.getBean("targetDataSources", Map.class);

        dataSources.forEach((tenant, dataSource) -> {
            LOGGER.info("Migrating tenant database (tenant={})", tenant);

            final var flyway = Flyway.configure()
                    .locations(migrationsLocation)
                    .dataSource(((DataSource) dataSource))
                    .load();

            final var migrationResult = flyway.migrate();
            LOGGER.info(
                    "Tenant database migrated (migrations={}, success={})",
                    migrationResult.migrationsExecuted,
                    migrationResult.success
            );
        });
    }
}
