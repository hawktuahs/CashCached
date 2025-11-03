package com.bt.accounts.exception;

public class AccountAlreadyClosedException extends RuntimeException {

    public AccountAlreadyClosedException(String message) {
        super(message);
    }
}
