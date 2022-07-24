package com.example.internal;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class TenantsDatabaseInitializerTest {

    @Container
    private static final PostgreSQLContainer<?> TENANT_A_DATABASE =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres"));

    @Container
    private static final PostgreSQLContainer<?> TENANT_B_DATABASE =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres"));

    private AnnotationConfigApplicationContext applicationContext;

    @BeforeEach
    public void beforeEach() {
        // Creating context with application properties already registered
        final var parentContext = new GenericApplicationContext();
        parentContext.getEnvironment()
                .getPropertySources()
                .addLast(new MapPropertySource("default", Map.ofEntries(
                        Map.entry("spring.datasource.driver-class-name", "org.postgresql.Driver"),
                        Map.entry("spring.flyway.locations", "db/migration"),
                        Map.entry("datasource.tenants.names", "tenant-a,tenant-b"),
                        Map.entry("datasource.tenant.tenant-a.url", TENANT_A_DATABASE.getJdbcUrl()),
                        Map.entry("datasource.tenant.tenant-a.username", TENANT_A_DATABASE.getUsername()),
                        Map.entry("datasource.tenant.tenant-a.password", TENANT_A_DATABASE.getPassword()),
                        Map.entry("datasource.tenant.tenant-b.url", TENANT_B_DATABASE.getJdbcUrl()),
                        Map.entry("datasource.tenant.tenant-b.username", TENANT_B_DATABASE.getUsername()),
                        Map.entry("datasource.tenant.tenant-b.password", TENANT_B_DATABASE.getPassword())
                )));
        parentContext.refresh();

        // Creating a new context that is going to read properties already registered
        // and create beans using them
        this.applicationContext = new AnnotationConfigApplicationContext();
        this.applicationContext.setParent(parentContext);
        this.applicationContext.register(TenantsConfiguration.class);
    }

    @Test
    void runningDatabaseMigrationsForEachDataSource() {
        // Telling ApplicationContext to create the dependency tree and run the system under test
        applicationContext.addApplicationListener(new TenantsDatabaseInitializer());
        applicationContext.refresh();

        final var expectedMigrationFiles = getMigrationFiles();
        final var migrationRunInTenantA = getMigrationsExecuted(TENANT_A_DATABASE);
        final var migrationRunInTenantB = getMigrationsExecuted(TENANT_B_DATABASE);

        assertThat(migrationRunInTenantA)
                .hasSize(1)
                .extracting(MigrationInfo::getScript)
                .containsExactlyElementsOf(expectedMigrationFiles);

        assertThat(migrationRunInTenantB)
                .hasSize(1)
                .extracting(MigrationInfo::getScript)
                .containsExactlyElementsOf(expectedMigrationFiles);
    }

    private List<MigrationInfo> getMigrationsExecuted(PostgreSQLContainer<?> container) {
        final var environment = applicationContext.getEnvironment();

        final var flyway = Flyway.configure()
                .locations(environment.getRequiredProperty("spring.flyway.locations"))
                .dataSource(container.getJdbcUrl(), container.getUsername(), container.getPassword())
                .load();

        return Arrays.asList(flyway.info().applied());
    }

    private List<String> getMigrationFiles() {
        final var environment = applicationContext.getEnvironment();
        final var migrationsLocation = environment.getRequiredProperty("spring.flyway.locations");
        final var classpath = Path.of("src", "main", "resources");

        try (var files = Files.list(classpath.resolve(migrationsLocation))) {
            return files.map(Path::getFileName)
                    .map(Path::toString)
                    .toList();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }
}