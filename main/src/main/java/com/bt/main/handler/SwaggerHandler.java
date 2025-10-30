package com.bt.main.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
public class SwaggerHandler {

    @Value("${services.customer.url}")
    private String customerUrl;

    @Value("${services.product.url}")
    private String productUrl;

    @Value("${services.fdcalculator.url}")
    private String fdCalculatorUrl;

    @Value("${services.accounts.url}")
    private String accountsUrl;

    private final WebClient webClient = WebClient.builder().build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public Mono<ServerResponse> getCustomerDocs(ServerRequest request) {
        return fetchAndModifyDocs(customerUrl + "/v3/api-docs");
    }

    public Mono<ServerResponse> getProductDocs(ServerRequest request) {
        return fetchAndModifyDocs(productUrl + "/v3/api-docs");
    }

    public Mono<ServerResponse> getFdDocs(ServerRequest request) {
        return fetchAndModifyDocs(fdCalculatorUrl + "/v3/api-docs");
    }

    public Mono<ServerResponse> getAccountsDocs(ServerRequest request) {
        return fetchAndModifyDocs(accountsUrl + "/v3/api-docs");
    }

    private Mono<ServerResponse> fetchAndModifyDocs(String url) {
        return webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(String.class)
                .flatMap(body -> {
                    try {
                        JsonNode root = objectMapper.readTree(body);
                        if (root.isObject()) {
                            ObjectNode obj = (ObjectNode) root;
                            ArrayNode servers = objectMapper.createArrayNode();
                            ObjectNode srv = objectMapper.createObjectNode();
                            srv.put("url", "/");
                            servers.add(srv);
                            obj.set("servers", servers);
                            obj.remove("host");
                            obj.remove("basePath");
                            obj.remove("schemes");
                            String modified = objectMapper.writeValueAsString(obj);
                            return ServerResponse.ok()
                                    .header("Content-Type", "application/json")
                                    .bodyValue(modified);
                        }
                        return ServerResponse.ok()
                                .header("Content-Type", "application/json")
                                .bodyValue(body);
                    } catch (Exception e) {
                        return ServerResponse.status(500).bodyValue("Error processing API docs");
                    }
                })
                .onErrorResume(e -> ServerResponse.status(503).bodyValue("Service unavailable"));
    }
}
