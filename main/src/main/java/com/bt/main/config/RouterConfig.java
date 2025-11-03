package com.bt.main.config;

import com.bt.main.handler.StaticResourceHandler;
import com.bt.main.handler.SwaggerHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;

@Configuration
public class RouterConfig {

    @Bean
    @Order(-1)
    public RouterFunction<ServerResponse> staticResourceRouter(StaticResourceHandler handler) {
        return RouterFunctions
                .route(GET("/"), handler::serveIndex)
                .andRoute(GET("/index.html"), handler::serveIndex)
                .andRoute(GET("/login"), handler::serveIndex)
                .andRoute(GET("/register"), handler::serveIndex)
                .andRoute(GET("/dashboard"), handler::serveIndex)
                .andRoute(GET("/profile"), handler::serveIndex)
                .andRoute(GET("/products"), handler::serveIndex)
                .andRoute(GET("/products/**"), handler::serveIndex)
                .andRoute(GET("/fd-calculator"), handler::serveIndex)
                .andRoute(GET("/accounts"), handler::serveIndex)
                .andRoute(GET("/accounts/**"), handler::serveIndex)
                .andRoute(GET("/static/**"), handler::serveStaticResource)
                .andRoute(GET("/assets/**"), handler::serveStaticResource)
                .andRoute(GET("/favicon.ico"), handler::serveStaticResource)
                .andRoute(GET("/vite.svg"), handler::serveStaticResource);
    }

    @Bean
    public RouterFunction<ServerResponse> swaggerRouter(SwaggerHandler handler) {
        return RouterFunctions
                .route(GET("/docs/customer"), handler::getCustomerDocs)
                .andRoute(GET("/docs/product"), handler::getProductDocs)
                .andRoute(GET("/docs/fd"), handler::getFdDocs)
                .andRoute(GET("/docs/accounts"), handler::getAccountsDocs);
    }
}
