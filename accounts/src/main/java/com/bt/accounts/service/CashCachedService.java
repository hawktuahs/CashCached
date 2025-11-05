package com.bt.accounts.service;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.bt.accounts.blockchain.CashCachedContract;
import com.bt.accounts.config.CashCachedProperties;
import com.bt.accounts.dto.CashCachedBalanceResponse;
import com.bt.accounts.dto.CashCachedIssueRequest;
import com.bt.accounts.dto.CashCachedRedeemRequest;
import com.bt.accounts.dto.CashCachedSummaryResponse;
import com.bt.accounts.dto.CashCachedTransferRequest;
import com.bt.accounts.entity.CashCachedLedgerEntry;
import com.bt.accounts.entity.CashCachedLedgerEntry.Operation;
import com.bt.accounts.entity.CashCachedWallet;
import com.bt.accounts.repository.CashCachedLedgerRepository;
import com.bt.accounts.repository.CashCachedWalletRepository;
import com.bt.accounts.exception.InvalidAccountDataException;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class CashCachedService {

    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final Map<String, BigDecimal> STATIC_RATE_TABLE = Map.of(
            "USD", new BigDecimal("1.00"),
            "KWD", new BigDecimal("0.31"),
            "INR", new BigDecimal("83.20"),
            "GBP", new BigDecimal("0.78"),
            "CAD", new BigDecimal("1.36"),
            "MXN", new BigDecimal("18.40"),
            "ZAR", new BigDecimal("18.20"));

    private final CashCachedContract contract;
    private final CashCachedProperties properties;
    private final CashCachedLedgerRepository ledgerRepository;
    private final CashCachedWalletRepository walletRepository;
    private final CustomerProfileClient customerProfileClient;

    private final AtomicReference<Integer> decimalsCache = new AtomicReference<>();

    @Transactional
    public CashCachedLedgerEntry issue(CashCachedIssueRequest request) {
        BigDecimal tokens = requireWholeTokens(request.getAmount());
        CashCachedWallet wallet = ensureWallet(request.getCustomerId());
        TransactionReceiptHolder receipt = mintToTreasury(tokens);
        wallet.setBalance(wallet.getBalance().add(tokens));
        walletRepository.save(wallet);
        return ledgerRepository.save(CashCachedLedgerEntry.builder()
                .customerId(request.getCustomerId())
                .changeAmount(tokens)
                .balanceAfter(wallet.getBalance())
                .operation(Operation.ISSUE)
                .transactionHash(receipt.transactionHash())
                .reference(request.getReference())
                .build());
    }

    @Transactional
    public CashCachedLedgerEntry recordContractLock(String customerId, BigDecimal amount, String reference) {
        BigDecimal tokens = requireWholeTokens(amount);
        CashCachedWallet wallet = ensureWallet(customerId);
        return ledgerRepository.save(CashCachedLedgerEntry.builder()
                .customerId(customerId)
                .changeAmount(BigDecimal.ZERO)
                .balanceAfter(wallet.getBalance())
                .operation(Operation.CONTRACT)
                .reference(reference + " (" + tokens + " tokens)")
                .build());
    }

    @Transactional
    public void mintForInterest(BigDecimal amount, String reference) {
        BigDecimal tokens = requireWholeTokens(amount);
        recordTreasuryIssuance(tokens, reference);
    }

    @Transactional
    public TransferResult transfer(CashCachedTransferRequest request) {
        BigDecimal amount = requireWholeTokens(request.getAmount());
        if (request.getFromCustomerId().equals(request.getToCustomerId())) {
            throw new IllegalArgumentException("Transfers require distinct customers");
        }
        CashCachedWallet fromWallet = ensureWallet(request.getFromCustomerId());
        CashCachedWallet toWallet = ensureWallet(request.getToCustomerId());
        if (fromWallet.getBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient balance for transfer");
        }
        fromWallet.setBalance(fromWallet.getBalance().subtract(amount));
        toWallet.setBalance(toWallet.getBalance().add(amount));
        walletRepository.save(fromWallet);
        walletRepository.save(toWallet);
        CashCachedLedgerEntry debit = ledgerRepository.save(CashCachedLedgerEntry.builder()
                .customerId(request.getFromCustomerId())
                .changeAmount(amount.negate())
                .balanceAfter(fromWallet.getBalance())
                .operation(Operation.TRANSFER_OUT)
                .reference(request.getReference())
                .build());

        CashCachedLedgerEntry credit = ledgerRepository.save(CashCachedLedgerEntry.builder()
                .customerId(request.getToCustomerId())
                .changeAmount(amount)
                .balanceAfter(toWallet.getBalance())
                .operation(Operation.TRANSFER_IN)
                .reference(request.getReference())
                .build());

        return new TransferResult(debit, credit);
    }

    @Transactional
    public CashCachedLedgerEntry redeem(CashCachedRedeemRequest request) {
        BigDecimal amount = requireWholeTokens(request.getAmount());
        CashCachedWallet wallet = ensureWallet(request.getCustomerId());
        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient balance for redemption");
        }
        TransactionReceiptHolder receipt = burnFromTreasury(amount);
        wallet.setBalance(wallet.getBalance().subtract(amount));
        walletRepository.save(wallet);
        return ledgerRepository.save(CashCachedLedgerEntry.builder()
                .customerId(request.getCustomerId())
                .changeAmount(amount.negate())
                .balanceAfter(wallet.getBalance())
                .operation(Operation.REDEEM)
                .transactionHash(receipt.transactionHash())
                .reference(request.getReference())
                .build());
    }

    @Transactional(readOnly = true)
    public List<CashCachedLedgerEntry> history(String customerId) {
        return ledgerRepository.findByCustomerIdOrderByCreatedAtDesc(customerId);
    }

    @Transactional(readOnly = true)
    public Page<CashCachedLedgerEntry> historyAll(Pageable pageable) {
        return ledgerRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    @Transactional(readOnly = true)
    public CashCachedBalanceResponse balance(String customerId, String authToken) {
        BigDecimal tokens = walletRepository.findByCustomerId(customerId)
                .map(CashCachedWallet::getBalance)
                .orElse(ZERO);

        CashCachedBalanceResponse response = new CashCachedBalanceResponse();
        response.setCustomerId(customerId);
        response.setBalance(tokens);
        response.setBaseCurrency(properties.getBaseCurrency());
        response.setBaseValue(tokens);

        Map<String, BigDecimal> rates = resolveRates();
        response.setRates(rates);

        String preferredCurrency = resolvePreferredCurrency(authToken, customerId);
        response.setTargetCurrency(preferredCurrency);

        BigDecimal targetRate = rates.getOrDefault(preferredCurrency, BigDecimal.ONE);
        response.setTargetValue(tokens.multiply(targetRate));

        return response;
    }

    @Transactional(readOnly = true)
    public BigDecimal totalSupplyOnChain() {
        try {
            BigInteger raw = contract.totalSupply().send();
            return fromTokenUnits(raw);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to read CashCached supply", e);
        }
    }

    @Transactional(readOnly = true)
    public BigDecimal ledgerTotal() {
        return ledgerRepository.findAll().stream()
                .map(CashCachedLedgerEntry::getChangeAmount)
                .reduce(ZERO, BigDecimal::add);
    }

    @Transactional
    public CashCachedLedgerEntry creditWallet(String customerId, BigDecimal amount, String reference) {
        BigDecimal tokens = requireWholeTokens(amount);
        CashCachedWallet wallet = ensureWallet(customerId);
        wallet.setBalance(wallet.getBalance().add(tokens));
        walletRepository.save(wallet);
        return ledgerRepository.save(CashCachedLedgerEntry.builder()
                .customerId(customerId)
                .changeAmount(tokens)
                .balanceAfter(wallet.getBalance())
                .operation(Operation.TRANSFER_IN)
                .reference(reference)
                .build());
    }

    @Transactional
    public CashCachedLedgerEntry debitWallet(String customerId, BigDecimal amount, String reference) {
        BigDecimal tokens = requireWholeTokens(amount);
        CashCachedWallet wallet = ensureWallet(customerId);
        if (wallet.getBalance().compareTo(tokens) < 0) {
            throw new InvalidAccountDataException("Insufficient CashCached balance");
        }
        wallet.setBalance(wallet.getBalance().subtract(tokens));
        walletRepository.save(wallet);
        return ledgerRepository.save(CashCachedLedgerEntry.builder()
                .customerId(customerId)
                .changeAmount(tokens.negate())
                .balanceAfter(wallet.getBalance())
                .operation(Operation.TRANSFER_OUT)
                .reference(reference)
                .build());
    }

    @Transactional(readOnly = true)
    public CashCachedSummaryResponse summary() {
        BigDecimal ledgerTotal = ledgerTotal();
        BigDecimal onChain = totalSupplyOnChain();
        CashCachedSummaryResponse response = new CashCachedSummaryResponse();
        response.setContractAddress(properties.getContractAddress());
        response.setTreasuryAddress(properties.getTreasuryAddress());
        response.setLedgerTotal(ledgerTotal);
        response.setOnChainSupply(onChain);
        response.setVariance(ledgerTotal.subtract(onChain));
        return response;
    }

    private CashCachedWallet ensureWallet(String customerId) {
        return walletRepository.findByCustomerId(customerId)
                .orElseGet(() -> walletRepository.save(CashCachedWallet.builder()
                        .customerId(customerId)
                        .balance(ZERO)
                        .build()));
    }

    private TransactionReceiptHolder mintToTreasury(BigDecimal amount) {
        try {
            BigInteger value = toTokenUnits(amount);
            String treasury = properties.getTreasuryAddress();
            String hash = contract.mint(treasury, value).send().getTransactionHash();
            return new TransactionReceiptHolder(hash);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to mint CashCached", e);
        }
    }

    private void recordTreasuryIssuance(BigDecimal tokens, String reference) {
        TransactionReceiptHolder receipt = mintToTreasury(tokens);
        String treasuryId = properties.getTreasuryAddress();
        CashCachedWallet treasuryWallet = ensureWallet(treasuryId);
        treasuryWallet.setBalance(treasuryWallet.getBalance().add(tokens));
        walletRepository.save(treasuryWallet);
        ledgerRepository.save(CashCachedLedgerEntry.builder()
                .customerId(treasuryId)
                .changeAmount(tokens)
                .balanceAfter(treasuryWallet.getBalance())
                .operation(Operation.ISSUE)
                .transactionHash(receipt.transactionHash())
                .reference(reference)
                .build());
    }

    private TransactionReceiptHolder burnFromTreasury(BigDecimal amount) {
        try {
            BigInteger value = toTokenUnits(amount);
            String hash = contract.burnFromTreasury(value).send().getTransactionHash();
            return new TransactionReceiptHolder(hash);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to burn CashCached", e);
        }
    }

    private int tokenDecimals() {
        Integer cached = decimalsCache.get();
        if (cached != null) {
            return cached;
        }
        synchronized (decimalsCache) {
            Integer doubleChecked = decimalsCache.get();
            if (doubleChecked != null) {
                return doubleChecked;
            }
            try {
                int resolved = contract.decimals().send().intValue();
                decimalsCache.set(resolved);
                return resolved;
            } catch (Exception e) {
                throw new IllegalStateException("Unable to read CashCached decimals", e);
            }
        }
    }

    private BigInteger toTokenUnits(BigDecimal amount) {
        int decimals = tokenDecimals();
        BigDecimal scaled = amount.setScale(decimals, RoundingMode.DOWN);
        return scaled.movePointRight(decimals).toBigIntegerExact();
    }

    private BigDecimal fromTokenUnits(BigInteger amount) {
        int decimals = tokenDecimals();
        BigDecimal raw = new BigDecimal(amount);
        return raw.movePointLeft(decimals);
    }

    private BigDecimal requireWholeTokens(BigDecimal amount) {
        if (amount == null) {
            throw new IllegalArgumentException("Token amount is required");
        }
        try {
            BigDecimal normalized = amount.stripTrailingZeros();
            BigDecimal scaled = normalized.setScale(0, RoundingMode.UNNECESSARY);
            if (scaled.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Token amount must be positive");
            }
            return scaled;
        } catch (ArithmeticException ex) {
            throw new IllegalArgumentException("CashCached tokens must be whole numbers", ex);
        }
    }

    @Getter
    @RequiredArgsConstructor
    public static class TransferResult {
        private final CashCachedLedgerEntry debitEntry;
        private final CashCachedLedgerEntry creditEntry;
    }

    @RequiredArgsConstructor
    private static class TransactionReceiptHolder {
        private final String transactionHash;

        public String transactionHash() {
            return transactionHash;
        }
    }

    private Map<String, BigDecimal> resolveRates() {
        Map<String, BigDecimal> rates = new HashMap<>();
        STATIC_RATE_TABLE.forEach((code, rate) -> rates.put(normalizeCurrency(code), rate.setScale(4, RoundingMode.HALF_UP)));

        String base = normalizeCurrency(properties.getBaseCurrency());
        rates.putIfAbsent(base, BigDecimal.ONE.setScale(4, RoundingMode.HALF_UP));

        properties.getSupportedCurrencies().stream()
                .map(this::normalizeCurrency)
                .forEach(code -> rates.putIfAbsent(code, STATIC_RATE_TABLE.getOrDefault(code,
                        BigDecimal.ONE.setScale(4, RoundingMode.HALF_UP))));

        return Collections.unmodifiableMap(rates);
    }

    private String normalizeCurrency(String code) {
        if (code == null || code.isBlank()) {
            return properties.getBaseCurrency();
        }
        return code.trim().toUpperCase();
    }

    private String resolvePreferredCurrency(String authToken, String customerId) {
        String baseCurrency = normalizeCurrency(properties.getBaseCurrency());
        if (!StringUtils.hasText(customerId)) {
            return baseCurrency;
        }

        return customerProfileClient.fetchPreferredCurrency(customerId, authToken)
                .map(this::normalizeCurrency)
                .filter(StringUtils::hasText)
                .orElse(baseCurrency);
    }

}
