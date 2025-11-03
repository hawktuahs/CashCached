package com.bt.accounts.exception;

public class ServiceIntegrationException extends RuntimeException {

    public ServiceIntegrationException(String message) {
        super(message);
    }

    public ServiceIntegrationException(String message, Throwable cause) {
        super(message, cause);
    }
}
