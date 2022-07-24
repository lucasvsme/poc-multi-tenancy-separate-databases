package com.example.product.api;

import com.example.testing.TenantDatabase;
import com.example.testing.TenantDatabases;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "datasource.tenants.names=company-z"
)
@TenantDatabases({
        @TenantDatabase(name = "company-z")
})
class ProductControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void productNameNull() {
        final var productRequest = new ProductRequest();
        productRequest.setName(null);

        createProduct(productRequest);
    }

    @Test
    void productNameEmpty() {
        final var productRequest = new ProductRequest();
        productRequest.setName("  ");

        createProduct(productRequest);
    }

    @Test
    void productNameTooLong() {
        final var productRequest = new ProductRequest();
        productRequest.setName("MoreThan15chars!");

        createProduct(productRequest);
    }

    private void createProduct(ProductRequest productRequest) {
        webTestClient.post()
                .uri("/products")
                .header("X-Tenant-Id", "company-z")
                .body(BodyInserters.fromValue(productRequest))
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.BAD_REQUEST);
    }
}