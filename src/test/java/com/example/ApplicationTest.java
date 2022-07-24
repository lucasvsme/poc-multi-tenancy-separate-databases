package com.example;

import com.example.product.api.ProductRequest;
import com.example.testing.TenantDatabase;
import com.example.testing.TenantDatabases;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;

import java.util.List;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "datasource.tenants.names=company-x,company-y"
)
@TenantDatabases({
        @TenantDatabase(name = "company-x"),
        @TenantDatabase(name = "company-y")
})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ApplicationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    @Order(1)
    void creatingProductsForTenantX() {
        final var paper = new ProductRequest();
        paper.setName("A4 Paper");

        final var pencil = new ProductRequest();
        pencil.setName("Pencil 1B");

        for (final var product : List.of(paper, pencil)) {
            webTestClient.post()
                    .uri("/products")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-Tenant-Id", "company-x")
                    .body(BodyInserters.fromValue(product))
                    .exchange()
                    .expectStatus().isEqualTo(HttpStatus.CREATED)
                    .expectHeader().exists(HttpHeaders.LOCATION)
                    .expectBody().isEmpty();
        }
    }

    @Test
    @Order(2)
    void creatingProductsForTenantY() {
        final var eraser = new ProductRequest();
        eraser.setName("Eraser");

        webTestClient.post()
                .uri("/products")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Tenant-Id", "company-y")
                .body(BodyInserters.fromValue(eraser))
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.CREATED)
                .expectHeader().exists(HttpHeaders.LOCATION)
                .expectBody().isEmpty();
    }

    @Test
    @Order(3)
    void findProductsFromEachTenant() {
        webTestClient.get()
                .uri("/products")
                .accept(MediaType.APPLICATION_JSON)
                .header("X-Tenant-Id", "company-x")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.OK)
                .expectBody()
                .jsonPath("$.products").isArray()
                .jsonPath("$.products[0].id").isEqualTo(1)
                .jsonPath("$.products[0].name").isEqualTo("A4 Paper")
                .jsonPath("$.products[1].id").isEqualTo(2)
                .jsonPath("$.products[1].name").isEqualTo("Pencil 1B");

        webTestClient.get()
                .uri("/products")
                .accept(MediaType.APPLICATION_JSON)
                .header("X-Tenant-Id", "company-y")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.OK)
                .expectBody()
                .jsonPath("$.products").isArray()
                .jsonPath("$.products[0].id").isEqualTo(1)
                .jsonPath("$.products[0].name").isEqualTo("Eraser");
    }

    @Test
    @Order(4)
    void errorFindingProductsFromUnknownTenant() {
        final var tenantId = "unknown";

        webTestClient.get()
                .uri("/products")
                .accept(MediaType.APPLICATION_JSON)
                .header("X-Tenant-Id", tenantId)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.BAD_REQUEST)
                .expectBody()
                .jsonPath("$.title").isEqualTo("Unknown database tenant")
                .jsonPath("$.detail").isEqualTo("Value of header X-Tenant-Id does not match a known database tenant")
                .jsonPath("$.tenantId").isEqualTo(tenantId);
    }
}
