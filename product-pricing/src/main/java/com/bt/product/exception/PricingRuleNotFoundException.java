package com.bt.product.exception;

public class PricingRuleNotFoundException extends RuntimeException {

    public PricingRuleNotFoundException(String message) {
        super(message);
    }

    public PricingRuleNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
