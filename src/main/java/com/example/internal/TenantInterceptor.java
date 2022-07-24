package com.example.internal;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import java.io.IOException;
import java.util.Optional;

public final class TenantInterceptor implements HandlerInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(TenantInterceptor.class);

    private static final String X_TENANT_ID = "X-Tenant-Id";

    private final ObjectMapper objectMapper;
    private final Tenants tenants;

    public TenantInterceptor(ObjectMapper objectMapper, Tenants tenants) {
        this.objectMapper = objectMapper;
        this.tenants = tenants;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        final var xTenantId = request.getHeader(X_TENANT_ID);
        final var tenant = Optional.ofNullable(xTenantId)
                .filter(tenantId -> tenants.getNames().contains(tenantId));

        if (tenant.isPresent()) {
            Tenant.set(tenant.get());
            LOGGER.debug("Handling request for tenant {}", Tenant.get());
            return true;
        }

        final var problemDetail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problemDetail.setTitle("Unknown database tenant");
        problemDetail.setDetail("Value of header X-Tenant-Id does not match a known database tenant");
        problemDetail.setProperty("tenantId", xTenantId);

        response.setStatus(HttpStatus.BAD_REQUEST.value());
        response.getWriter().print(objectMapper.writeValueAsString(problemDetail));
        return false;
    }

    @Override
    public void postHandle(HttpServletRequest request,
                           HttpServletResponse response,
                           Object handler,
                           ModelAndView modelAndView) {
        Tenant.unset();
        LOGGER.debug("Removed tenant assigned previously before sending response to client");
    }
}
