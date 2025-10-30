package com.bt.product.event;

/**
 * Kafka topics configuration for inter-service communication.
 */
public class KafkaTopics {
    // Customer Service Topics
    public static final String CUSTOMER_CREATED = "customer.created";
    public static final String CUSTOMER_UPDATED = "customer.updated";
    public static final String CUSTOMER_VALIDATION_REQUEST = "customer.validation.request";
    public static final String CUSTOMER_VALIDATION_RESPONSE = "customer.validation.response";

    // Product Service Topics
    public static final String PRODUCT_CREATED = "product.created";
    public static final String PRODUCT_UPDATED = "product.updated";
    public static final String PRODUCT_DETAILS_REQUEST = "product.details.request";
    public static final String PRODUCT_DETAILS_RESPONSE = "product.details.response";

    // FD Calculator Service Topics
    public static final String FD_CALCULATION_REQUEST = "fd.calculation.request";
    public static final String FD_CALCULATION_RESPONSE = "fd.calculation.response";
    public static final String FD_HISTORY_REQUEST = "fd.history.request";
    public static final String FD_HISTORY_RESPONSE = "fd.history.response";

    // Account Service Topics
    public static final String ACCOUNT_CREATED = "account.created";
    public static final String ACCOUNT_UPDATED = "account.updated";

    private KafkaTopics() {
        // Utility class
    }
}
