package com.bt.main.gateway;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.io.IOException;
import java.net.URI;
import java.util.Enumeration;

@RestController
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class ProxyController {

    @Value("${services.customer.url}")
    private String customerBase;
    @Value("${services.product.url}")
    private String productBase;
    @Value("${services.fdcalculator.url}")
    private String fdBase;
    @Value("${services.accounts.url}")
    private String accountsBase;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @RequestMapping(path = {
            "/api/auth/**",
            "/api/customer/**",
            "/api/v1/product/**",
            "/api/v1/pricing-rule/**",
            "/api/fd/**",
            "/api/accounts/**"
    }, method = { RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE })
    public ResponseEntity<byte[]> proxyApi(HttpServletRequest request, @RequestBody(required = false) byte[] body)
            throws IOException {
        String path = request.getRequestURI();
        String targetBase = resolveBaseUrl(path);
        String targetUrl = targetBase + path;
        if (request.getQueryString() != null && !request.getQueryString().isEmpty()) {
            targetUrl += "?" + request.getQueryString();
        }
        return forward(request, body, targetUrl);
    }

    @GetMapping({
            "/docs/customer",
            "/docs/product",
            "/docs/fd",
            "/docs/accounts"
    })
    public ResponseEntity<byte[]> proxyDocs(HttpServletRequest request) throws IOException {
        String path = request.getRequestURI();
        String targetPath = "/v3/api-docs";
        String targetBase;
        if (path.endsWith("customer"))
            targetBase = customerBase;
        else if (path.endsWith("product"))
            targetBase = productBase;
        else if (path.endsWith("fd"))
            targetBase = fdBase;
        else
            targetBase = accountsBase;
        ResponseEntity<byte[]> resp = forward(request, null, targetBase + targetPath);
        if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null)
            return resp;
        JsonNode root = objectMapper.readTree(resp.getBody());
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
            byte[] modified = objectMapper.writeValueAsBytes(obj);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            return new ResponseEntity<>(modified, headers, HttpStatus.OK);
        }
        return resp;
    }

    private String resolveBaseUrl(String path) {
        if (path.startsWith("/api/auth") || path.startsWith("/api/customer"))
            return customerBase;
        if (path.startsWith("/api/v1/product") || path.startsWith("/api/v1/pricing-rule"))
            return productBase;
        if (path.startsWith("/api/fd"))
            return fdBase;
        if (path.startsWith("/api/accounts"))
            return accountsBase;
        throw new IllegalArgumentException("Unsupported path: " + path);
    }

    private ResponseEntity<byte[]> forward(HttpServletRequest request, byte[] body, String targetUrl)
            throws IOException {
        HttpMethod method = HttpMethod.valueOf(request.getMethod());
        HttpHeaders headers = extractHeaders(request);
        if (body == null) {
            body = request.getInputStream().readAllBytes();
        }
        HttpEntity<byte[]> httpEntity = new HttpEntity<>(body, headers);
        ResponseEntity<byte[]> response = restTemplate.exchange(URI.create(targetUrl), method, httpEntity,
                byte[].class);

        HttpHeaders respHeaders = new HttpHeaders();
        MediaType ct = response.getHeaders().getContentType();
        if (ct != null)
            respHeaders.setContentType(ct);
        response.getHeaders().forEach((k, v) -> {
            if (HttpHeaders.SET_COOKIE.equalsIgnoreCase(k) || HttpHeaders.LOCATION.equalsIgnoreCase(k)) {
                respHeaders.put(k, v);
            }
        });
        return new ResponseEntity<>(response.getBody(), respHeaders, response.getStatusCode());
    }

    private HttpHeaders extractHeaders(HttpServletRequest request) {
        HttpHeaders headers = new HttpHeaders();
        Enumeration<String> names = request.getHeaderNames();
        while (names.hasMoreElements()) {
            String n = names.nextElement();
            Enumeration<String> vals = request.getHeaders(n);
            while (vals.hasMoreElements())
                headers.add(n, vals.nextElement());
        }
        headers.remove(HttpHeaders.HOST);
        return headers;
    }
}
