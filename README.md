# POC: Multi-tenancy Separate Databases

It demonstrates how to implement a multi-tenant REST API that persists data in different databases.

The goal is to develop a REST API capable of persist and retrieve a product catalog. Each product must have a name and a
unique ID generated automatically by a database sequence that is going to be assigned to the product when the client
requests a new product to be created.

The application should be able to switch among different data sources based on
which client is sending the request to create or find products based on an HTTP header, therefore not mixing products
from different clients by using different database instances. An error should be returned to clients requesting data
without informing a known tenant.

All tenant configuration should be defined outside the application source code using property files, and we must be able
to add or remove tenants without changing the source code.

The Web service is implemented using Spring MVC, data is persisted on Postgres databases managed by Flyway using
Spring Data JPA. The source code is managed by JUnit with databases provisioned in containers managed by TestContainers
and no manual configuration is required to run automated tests.

## How to run

| Description                    | Command                                             |
|:-------------------------------|:----------------------------------------------------|
| Run tests                      | `./gradlew test`                                    |
| Run application                | `./gradlew bootRun`                                 |
| Provision database¹            | `docker-compose up --detach`                        |
| Destroy database¹              | `docker-compose down --volumes`                     |

> ¹Required for manual testing only, automated tests provision and destroy a database automatically. Must run
> inside `infrastructure` folder.

## Preview

Overview of this multi-tenant approach:

```mermaid
flowchart TB
api[Product API]

tenant_a((Tenant A))
tenant_a_database[(Tenant A\nDataSource)]

tenant_b((Tenant B))
tenant_b_database[(Tenant B\nDataSource)]

tenant_n((Tenant N))
tenant_n_database[(Tenant N\nDataSource)]

tenant_a -- GET /products\nX-Tenant-Id: tenant-a --> api
tenant_b -- GET /products\nX-Tenant-Id: tenant-b --> api
tenant_n -. GET /products\nX-Tenant-Id: tenant-n -.-> api

api -- public.PRODUCT --> tenant_a_database
api -- public.PRODUCT --> tenant_b_database
api -. public.PRODUCT -.-> tenant_n_database
```

Logging statements from application startup during automated tests:

```text
2022-07-24T17:35:56.649-03:00  INFO 134904 --- [    Test worker] c.example.testing.TenantDatabaseFactory  : Database tenant provisioned (tenant=company-x, properties={datasource.tenant.company-x.password=test, datasource.tenant.company-x.url=jdbc:postgresql://localhost:49608/test?loggerLevel=OFF, datasource.tenant.company-x.username=test})
2022-07-24T17:35:57.709-03:00  INFO 134904 --- [    Test worker] c.example.testing.TenantDatabaseFactory  : Database tenant provisioned (tenant=company-y, properties={datasource.tenant.company-y.password=test, datasource.tenant.company-y.url=jdbc:postgresql://localhost:49609/test?loggerLevel=OFF, datasource.tenant.company-y.username=test})
2022-07-24T17:35:57.719-03:00  INFO 134904 --- [    Test worker] com.example.ApplicationTest              : Starting ApplicationTest using Java 18.0.2 on pc with PID 134904 (started by lucas in /home/lucas/Documents/projects/poc-multi-tenancy-separate-databases)
2022-07-24T17:35:57.719-03:00 DEBUG 134904 --- [    Test worker] com.example.ApplicationTest              : Running with Spring Boot v3.0.0-M4, Spring v6.0.0-M5
2022-07-24T17:35:57.720-03:00  INFO 134904 --- [    Test worker] com.example.ApplicationTest              : No active profile set, falling back to 1 default profile: "default"
2022-07-24T17:35:59.235-03:00  INFO 134904 --- [    Test worker] c.e.internal.TenantsDatabaseInitializer  : Initializing tenant databases
2022-07-24T17:35:59.236-03:00  INFO 134904 --- [    Test worker] c.e.internal.TenantsDatabaseInitializer  : Migrating tenant database (tenant=company-x)
2022-07-24T17:35:59.399-03:00  INFO 134904 --- [    Test worker] c.e.internal.TenantsDatabaseInitializer  : Tenant database migrated (migrations=1, success=true)
2022-07-24T17:35:59.399-03:00  INFO 134904 --- [    Test worker] c.e.internal.TenantsDatabaseInitializer  : Migrating tenant database (tenant=company-y)
2022-07-24T17:35:59.434-03:00  INFO 134904 --- [    Test worker] c.e.internal.TenantsDatabaseInitializer  : Tenant database migrated (migrations=1, success=true)
2022-07-24T17:35:59.436-03:00  INFO 134904 --- [    Test worker] com.example.ApplicationTest              : Started ApplicationTest in 4.653 seconds (process running for 5.363)
2022-07-24T17:35:59.838-03:00 DEBUG 134904 --- [o-auto-1-exec-1] com.example.internal.TenantInterceptor   : Handling request for tenant company-x
2022-07-24T17:35:59.881-03:00  INFO 134904 --- [o-auto-1-exec-1] c.example.product.api.ProductController  : Creating new product (request=ProductRequest(name=A4 Paper))
2022-07-24T17:35:59.903-03:00  INFO 134904 --- [o-auto-1-exec-1] c.example.product.api.ProductController  : New product created (product=Product(id=1, name=A4 Paper))
2022-07-24T17:35:59.907-03:00 DEBUG 134904 --- [o-auto-1-exec-1] com.example.internal.TenantInterceptor   : Removed tenant assigned previously before sending response to client
2022-07-24T17:35:59.929-03:00 DEBUG 134904 --- [o-auto-1-exec-2] com.example.internal.TenantInterceptor   : Handling request for tenant company-x
2022-07-24T17:35:59.930-03:00  INFO 134904 --- [o-auto-1-exec-2] c.example.product.api.ProductController  : Creating new product (request=ProductRequest(name=Pencil 1B))
2022-07-24T17:35:59.938-03:00  INFO 134904 --- [o-auto-1-exec-2] c.example.product.api.ProductController  : New product created (product=Product(id=2, name=Pencil 1B))
2022-07-24T17:35:59.939-03:00 DEBUG 134904 --- [o-auto-1-exec-2] com.example.internal.TenantInterceptor   : Removed tenant assigned previously before sending response to client
2022-07-24T17:35:59.961-03:00 DEBUG 134904 --- [o-auto-1-exec-3] com.example.internal.TenantInterceptor   : Handling request for tenant company-y
2022-07-24T17:35:59.963-03:00  INFO 134904 --- [o-auto-1-exec-3] c.example.product.api.ProductController  : Creating new product (request=ProductRequest(name=Eraser))
2022-07-24T17:35:59.965-03:00  INFO 134904 --- [o-auto-1-exec-3] c.example.product.api.ProductController  : New product created (product=Product(id=1, name=Eraser))
2022-07-24T17:35:59.966-03:00 DEBUG 134904 --- [o-auto-1-exec-3] com.example.internal.TenantInterceptor   : Removed tenant assigned previously before sending response to client
2022-07-24T17:35:59.974-03:00 DEBUG 134904 --- [o-auto-1-exec-4] com.example.internal.TenantInterceptor   : Handling request for tenant company-x
2022-07-24T17:35:59.974-03:00  INFO 134904 --- [o-auto-1-exec-4] c.example.product.api.ProductController  : Finding all existing products
2022-07-24T17:36:00.025-03:00  INFO 134904 --- [o-auto-1-exec-4] c.example.product.api.ProductController  : Returning all products found (products=ProductsResponse(products=[ProductResponse(id=1, name=A4 Paper), ProductResponse(id=2, name=Pencil 1B)]))
2022-07-24T17:36:00.031-03:00 DEBUG 134904 --- [o-auto-1-exec-4] com.example.internal.TenantInterceptor   : Removed tenant assigned previously before sending response to client
2022-07-24T17:36:00.063-03:00 DEBUG 134904 --- [o-auto-1-exec-5] com.example.internal.TenantInterceptor   : Handling request for tenant company-y
2022-07-24T17:36:00.064-03:00  INFO 134904 --- [o-auto-1-exec-5] c.example.product.api.ProductController  : Finding all existing products
2022-07-24T17:36:00.066-03:00  INFO 134904 --- [o-auto-1-exec-5] c.example.product.api.ProductController  : Returning all products found (products=ProductsResponse(products=[ProductResponse(id=1, name=Eraser)]))
2022-07-24T17:36:00.068-03:00 DEBUG 134904 --- [o-auto-1-exec-5] com.example.internal.TenantInterceptor   : Removed tenant assigned previously before sending response to client
```