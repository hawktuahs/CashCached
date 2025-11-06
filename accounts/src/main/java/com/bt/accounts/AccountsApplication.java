package com.bt.accounts;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import com.bt.accounts.config.AuthServiceProperties;
import com.bt.accounts.config.CashCachedProperties;
import com.bt.accounts.config.PricingServiceProperties;

@SpringBootApplication
@EnableConfigurationProperties({CashCachedProperties.class, PricingServiceProperties.class, AuthServiceProperties.class})
public class AccountsApplication {

    public static void main(String[] args) {
        SpringApplication.run(AccountsApplication.class, args);
    }

}
