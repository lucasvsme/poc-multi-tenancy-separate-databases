package com.example.product;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class ProductRepositoryTest {

    @Container
    private static final PostgreSQLContainer<?> DATABASE_CONTAINER =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres"));

    @DynamicPropertySource
    private static void setApplicationProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.flyway.enabled", () -> true);
        registry.add("spring.datasource.url", DATABASE_CONTAINER::getJdbcUrl);
        registry.add("spring.datasource.username", DATABASE_CONTAINER::getUsername);
        registry.add("spring.datasource.password", DATABASE_CONTAINER::getPassword);
        registry.add("spring.datasource.driver-class-name", DATABASE_CONTAINER::getDriverClassName);
    }

    @Autowired
    private ProductRepository productRepository;

    @Test
    void productIdIsGeneratedSequentially() {
        // Making sure we don't have any records in our database that may mislead test result
        assertEquals(0L, productRepository.count(), "None products should be persisted");

        // Creating two products without assigning IDs to them
        final var productA = new Product();
        productA.setId(null);
        productA.setName("Notebook");
        final var productASaved = productRepository.save(productA);

        final var productB = new Product();
        productB.setId(null);
        productB.setName("Pen");
        final var productBSaved = productRepository.save(productB);

        // Asserting both products created have sequential IDs
        assertEquals(1L, productASaved.getId());
        assertEquals(productA.getName(), productASaved.getName());

        assertEquals(2L, productBSaved.getId());
        assertEquals(productB.getName(), productBSaved.getName());

        assertEquals(2L, productRepository.count(), "Test should finish with two products persisted");
    }
}
