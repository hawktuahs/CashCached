package com.bt.accounts.service;

import com.bt.accounts.config.AuthServiceProperties;
import com.bt.accounts.dto.ApiResponse;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
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

@Service
@Slf4j
public class CustomerProfileClient {

    private static final ParameterizedTypeReference<ApiResponse<CustomerProfileDto>> RESPONSE_TYPE =
            new ParameterizedTypeReference<>() {
            };

    private final RestTemplate restTemplate;
    private final AuthServiceProperties authProperties;
    private final ServiceTokenProvider serviceTokenProvider;

    public CustomerProfileClient(RestTemplate restTemplate, AuthServiceProperties authProperties,
            ServiceTokenProvider serviceTokenProvider) {
        this.restTemplate = restTemplate;
        this.authProperties = authProperties;
        this.serviceTokenProvider = serviceTokenProvider;
    }

    public Optional<String> fetchPreferredCurrency(String customerId, String authToken) {
        if (!StringUtils.hasText(customerId)) {
            return Optional.empty();
        }

        Long id;
        try {
            id = Long.parseLong(customerId.trim());
        } catch (NumberFormatException ex) {
            log.warn("Invalid customer id supplied for preferred currency lookup: {}", customerId);
            return Optional.empty();
        }

        String baseUrl = StringUtils.trimTrailingCharacter(Objects.toString(authProperties.getUrl(), "").trim(), '/');
        if (!StringUtils.hasText(baseUrl)) {
            log.warn("Customer service URL is not configured; falling back to base currency");
            return Optional.empty();
        }

        String url = baseUrl + "/api/v1/customers/" + id;

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.setBearerAuth(resolveBearerToken(authToken));

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<ApiResponse<CustomerProfileDto>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    RESPONSE_TYPE);

            ApiResponse<CustomerProfileDto> body = response.getBody();
            if (body == null) {
                log.warn("Customer profile response was empty for id {}", id);
                return Optional.empty();
            }
            if (!Boolean.TRUE.equals(body.getSuccess())) {
                log.warn("Customer profile lookup for id {} failed: {}", id, body.getMessage());
                return Optional.empty();
            }
            CustomerProfileDto profile = body.getData();
            if (profile == null) {
                return Optional.empty();
            }
            return Optional.ofNullable(profile.getPreferredCurrency());
        } catch (RestClientException ex) {
            log.warn("Customer profile fetch failed for id {}: {}", id, ex.getMessage());
            return Optional.empty();
        }
    }

    private String resolveBearerToken(String authToken) {
        if (StringUtils.hasText(authToken)) {
            String candidate = authToken.trim();
            if (candidate.regionMatches(true, 0, "Bearer ", 0, 7)) {
                return candidate.substring(7).trim();
            }
            return candidate;
        }
        String serviceToken = serviceTokenProvider.getBearerToken();
        if (StringUtils.hasText(serviceToken)) {
            String candidate = serviceToken.trim();
            if (candidate.regionMatches(true, 0, "Bearer ", 0, 7)) {
                return candidate.substring(7).trim();
            }
            return candidate;
        }
        throw new IllegalStateException("Unable to resolve authorization token for customer service");
    }

    private static class CustomerProfileDto {
        private String preferredCurrency;

        public String getPreferredCurrency() {
            return preferredCurrency;
        }

        public void setPreferredCurrency(String preferredCurrency) {
            this.preferredCurrency = preferredCurrency;
        }
    }
}
