package com.bt.accounts.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "cashcached")
public class CashCachedProperties {

    private String rpcUrl;
    private String contractAddress;
    private String treasuryPrivateKey;
    private String treasuryAddress;
    private String baseCurrency = "KWD";
    private List<String> supportedCurrencies = new ArrayList<>(List.of("USD", "INR", "GBP", "CAD", "MXN", "ZAR"));
    private String exchangeRateUrl = "https://api.exchangerate.host/latest";

    public String getRpcUrl() {
        return rpcUrl;
    }

    public void setRpcUrl(String rpcUrl) {
        this.rpcUrl = rpcUrl;
    }

    public String getContractAddress() {
        return contractAddress;
    }

    public void setContractAddress(String contractAddress) {
        this.contractAddress = contractAddress;
    }

    public String getTreasuryPrivateKey() {
        return treasuryPrivateKey;
    }

    public void setTreasuryPrivateKey(String treasuryPrivateKey) {
        this.treasuryPrivateKey = treasuryPrivateKey;
    }

    public String getTreasuryAddress() {
        return treasuryAddress;
    }

    public void setTreasuryAddress(String treasuryAddress) {
        this.treasuryAddress = treasuryAddress;
    }

    public String getBaseCurrency() {
        return baseCurrency;
    }

    public void setBaseCurrency(String baseCurrency) {
        this.baseCurrency = baseCurrency;
    }

    public List<String> getSupportedCurrencies() {
        return supportedCurrencies;
    }

    public void setSupportedCurrencies(List<String> supportedCurrencies) {
        this.supportedCurrencies = supportedCurrencies;
    }

    public String getExchangeRateUrl() {
        return exchangeRateUrl;
    }

    public void setExchangeRateUrl(String exchangeRateUrl) {
        this.exchangeRateUrl = exchangeRateUrl;
    }
}
