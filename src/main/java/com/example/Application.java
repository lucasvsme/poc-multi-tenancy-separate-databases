package com.example;

import com.example.internal.TenantsContextInitializer;
import com.example.internal.TenantsDatabaseInitializer;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        new SpringApplicationBuilder(Application.class)
                .initializers(new TenantsContextInitializer())
                .listeners(new TenantsDatabaseInitializer())
                .run(args);
    }
}
