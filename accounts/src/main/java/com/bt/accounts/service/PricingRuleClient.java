package com.bt.accounts.service;

import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.bt.accounts.config.PricingServiceProperties;
import com.bt.accounts.dto.ApiResponse;
import com.bt.accounts.dto.PricingRuleDto;
import com.bt.accounts.entity.FdAccount;
import com.bt.accounts.exception.ServiceIntegrationException;

@Service
public class PricingRuleClient {

    private static final Logger log = LoggerFactory.getLogger(PricingRuleClient.class);

    private static final ParameterizedTypeReference<ApiResponse<List<PricingRuleDto>>> RESPONSE_TYPE =
            new ParameterizedTypeReference<>() {
            };

    private final RestTemplate restTemplate;
    private final PricingServiceProperties properties;
    private final ServiceTokenProvider serviceTokenProvider;

    public PricingRuleClient(RestTemplate restTemplate, PricingServiceProperties properties,
            ServiceTokenProvider serviceTokenProvider) {
        this.restTemplate = restTemplate;
        this.properties = properties;
        this.serviceTokenProvider = serviceTokenProvider;
    }

    public List<PricingRuleDto> fetchActiveRules(FdAccount account, String authToken) {
        if (account == null) {
            return Collections.emptyList();
        }
        Long productId = account.getProductRefId();
        if (productId == null) {
            log.debug("Account {} missing product reference id, skipping pricing rule fetch", account.getAccountNo());
            return Collections.emptyList();
        }

        String baseUrl = properties.getUrl();
        if (!StringUtils.hasText(baseUrl)) {
            throw new ServiceIntegrationException("Pricing service base URL is not configured");
        }

        String url = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .path("/api/v1/pricing-rule/product/{productId}/active")
                .buildAndExpand(productId)
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.setBearerAuth(resolveBearerToken(authToken));

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<ApiResponse<List<PricingRuleDto>>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    RESPONSE_TYPE);

            ApiResponse<List<PricingRuleDto>> body = response.getBody();
            if (body == null) {
                log.warn("Pricing rule response for product {} was empty", productId);
                return Collections.emptyList();
            }
            if (!Boolean.TRUE.equals(body.getSuccess())) {
                log.warn("Pricing rule request for product {} failed: {}", productId, body.getMessage());
                return Collections.emptyList();
            }
            List<PricingRuleDto> rules = body.getData();
            return rules != null ? rules : Collections.emptyList();
        } catch (RestClientException ex) {
            log.warn("Pricing rule fetch failed for product {}: {}", productId, ex.getMessage());
            throw new ServiceIntegrationException("Failed to fetch pricing rules", ex);
        }
    }

    private String resolveBearerToken(String authToken) {
        if (StringUtils.hasText(authToken)) {
            return authToken.trim().startsWith("Bearer ") ? authToken.trim().substring(7) : authToken.trim();
        }
        if (StringUtils.hasText(properties.getToken())) {
            String candidate = properties.getToken().trim();
            return candidate.startsWith("Bearer ") ? candidate.substring(7) : candidate;
        }
        return extractRawToken(serviceTokenProvider.getBearerToken());
    }

    private String extractRawToken(String bearerToken) {
        if (!StringUtils.hasText(bearerToken)) {
            throw new ServiceIntegrationException("Missing authorization token for pricing service");
        }
        String candidate = bearerToken.trim();
        if (candidate.startsWith("Bearer ")) {
            candidate = candidate.substring(7);
        }
        if (!StringUtils.hasText(candidate)) {
            throw new ServiceIntegrationException("Missing authorization token for pricing service");
        }
        return candidate;
    }
}
