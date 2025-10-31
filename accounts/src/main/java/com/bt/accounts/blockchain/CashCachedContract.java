package com.bt.accounts.blockchain;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;

import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.protocol.Web3j;
import org.web3j.tx.Contract;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.ContractGasProvider;
import org.web3j.protocol.core.RemoteCall;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.core.RemoteFunctionCall;

public class CashCachedContract extends Contract {

    private static final String BINARY = "";
    private static final String FUNC_MINT = "mint";
    private static final String FUNC_BURN_FROM_TREASURY = "burnFromTreasury";
    private static final String FUNC_BALANCE_OF = "balanceOf";
    private static final String FUNC_TOTAL_SUPPLY = "totalSupply";
    private static final String FUNC_TRANSFER = "transfer";
    private static final String FUNC_DECIMALS = "decimals";

    protected CashCachedContract(String contractAddress, Web3j web3j, TransactionManager transactionManager,
            ContractGasProvider gasProvider) {
        super(BINARY, contractAddress, web3j, transactionManager, gasProvider);
    }

    public static CashCachedContract load(String contractAddress, Web3j web3j, TransactionManager transactionManager,
            ContractGasProvider contractGasProvider) {
        return new CashCachedContract(contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public RemoteFunctionCall<TransactionReceipt> mint(String receiver, BigInteger amount) {
        final Function function = new Function(
                FUNC_MINT,
                Arrays.asList(new Address(receiver), new Uint256(amount)),
                Collections.emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> burnFromTreasury(BigInteger amount) {
        final Function function = new Function(
                FUNC_BURN_FROM_TREASURY,
                Arrays.asList(new Uint256(amount)),
                Collections.emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> transfer(String to, BigInteger amount) {
        final Function function = new Function(
                FUNC_TRANSFER,
                Arrays.asList(new Address(to), new Uint256(amount)),
                Collections.emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<BigInteger> balanceOf(String holder) {
        final Function function = new Function(
                FUNC_BALANCE_OF,
                Arrays.asList(new Address(holder)),
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {
                }));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<BigInteger> totalSupply() {
        final Function function = new Function(
                FUNC_TOTAL_SUPPLY,
                Collections.emptyList(),
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {
                }));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<BigInteger> decimals() {
        final Function function = new Function(
                FUNC_DECIMALS,
                Collections.emptyList(),
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {
                }));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteCall<String> getContractAddressCall() {
        return new RemoteCall<>(() -> getContractAddress());
    }
}
