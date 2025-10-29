package com.bt.accounts.config;

import java.math.BigInteger;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.RawTransactionManager;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.ContractGasProvider;
import org.web3j.tx.gas.StaticGasProvider;

import com.bt.accounts.blockchain.CashCachedContract;

@Configuration
public class CashCachedWeb3Config {

    @Bean
    public Web3j cashCachedWeb3Client(CashCachedProperties properties) {
        return Web3j.build(new HttpService(properties.getRpcUrl()));
    }

    @Bean
    public Credentials cashCachedCredentials(CashCachedProperties properties) {
        return Credentials.create(properties.getTreasuryPrivateKey());
    }

    @Bean
    public TransactionManager cashCachedTransactionManager(Web3j cashCachedWeb3Client, Credentials cashCachedCredentials) {
        return new RawTransactionManager(cashCachedWeb3Client, cashCachedCredentials, 80002);
    }

    @Bean
    public ContractGasProvider cashCachedGasProvider() {
        BigInteger gasPrice = BigInteger.valueOf(30_000_000_000L);
        BigInteger gasLimit = BigInteger.valueOf(3_000_000L);
        return new StaticGasProvider(gasPrice, gasLimit);
    }

    @Bean
    public CashCachedContract cashCachedContract(CashCachedProperties properties, Web3j cashCachedWeb3Client,
            TransactionManager cashCachedTransactionManager, ContractGasProvider cashCachedGasProvider) {
        return CashCachedContract.load(properties.getContractAddress(), cashCachedWeb3Client,
                cashCachedTransactionManager, cashCachedGasProvider);
    }
}
