package com.bt.accounts.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "services.product")
public class PricingServiceProperties {

    /**
     * Base URL for the product-pricing service.
     */
    private String url;

    /**
     * Optional static bearer token for service-to-service auth.
     */
    private String token;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

}
