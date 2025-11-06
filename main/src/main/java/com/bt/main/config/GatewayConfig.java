package com.bt.main.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayConfig {

        @Value("${services.customer.url}")
        private String customerUrl;

        @Value("${services.product.url}")
        private String productUrl;

        @Value("${services.fdcalculator.url}")
        private String fdCalculatorUrl;

        @Value("${services.accounts.url}")
        private String accountsUrl;

        @Bean
        public RouteLocator gatewayRoutes(RouteLocatorBuilder builder) {
                return builder.routes()
                                .route("customer_auth", r -> r.path("/api/auth/**")
                                                .uri(customerUrl))
                                .route("customer_api", r -> r.path("/api/customer/**", "/api/v1/customer/**")
                                                .uri(customerUrl))
                                .route("customer_docs", r -> r.path("/docs/customer")
                                                .filters(f -> f.rewritePath("/docs/customer", "/v3/api-docs"))
                                                .uri(customerUrl))
                                .route("product_api", r -> r.path("/api/v1/product/**", "/api/v1/pricing-rule/**")
                                                .uri(productUrl))
                                .route("product_docs", r -> r.path("/docs/product")
                                                .filters(f -> f.rewritePath("/docs/product", "/v3/api-docs"))
                                                .uri(productUrl))
                                .route("fd_calculator_api", r -> r.path("/api/fd/**")
                                                .uri(fdCalculatorUrl))
                                .route("fd_calculator_docs", r -> r.path("/docs/fd")
                                                .filters(f -> f.rewritePath("/docs/fd", "/v3/api-docs"))
                                                .uri(fdCalculatorUrl))
                                .route("accounts_api",
                                                r -> r.path("/api/accounts/**", "/api/v1/accounts/**",
                                                                "/api/v2/accounts/**", "/api/financials/**")
                                                                .uri(accountsUrl))
                                .route("accounts_docs", r -> r.path("/docs/accounts")
                                                .filters(f -> f.rewritePath("/docs/accounts", "/v3/api-docs"))
                                                .uri(accountsUrl))
                                .build();
        }
}
