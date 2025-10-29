package com.bt.accounts.service;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import com.bt.accounts.blockchain.CashCachedContract;
import com.bt.accounts.config.CashCachedProperties;
import com.bt.accounts.client.CustomerServiceClient;
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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CashCachedService {

    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private final CashCachedContract contract;
    private final CashCachedProperties properties;
    private final CashCachedLedgerRepository ledgerRepository;
    private final CashCachedWalletRepository walletRepository;
    private final CustomerServiceClient customerServiceClient;

    private final AtomicReference<Integer> decimalsCache = new AtomicReference<>();
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, RateSnapshot> rateCache = new ConcurrentHashMap<>();

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
    public void mintForInterest(BigDecimal amount, String reference) {
        BigDecimal tokens = requireWholeTokens(amount);
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
        String base = normalizeCurrency(properties.getBaseCurrency());
        RateSnapshot snapshot = rateCache.get(base);
        if (snapshot != null && Duration.between(snapshot.fetchedAt(), LocalDateTime.now()).toHours() < 24) {
            return snapshot.rates();
        }
        Map<String, BigDecimal> fetched = fetchRates(base);
        rateCache.put(base, new RateSnapshot(fetched, LocalDateTime.now()));
        return fetched;
    }

    private Map<String, BigDecimal> fetchRates(String base) {
        try {
            String symbols = String.join(",", properties.getSupportedCurrencies().stream()
                    .map(this::normalizeCurrency)
                    .filter(code -> !code.equalsIgnoreCase(base))
                    .toList());
            String url = "%s?base=%s&symbols=%s".formatted(properties.getExchangeRateUrl(), base, symbols);
            String payload = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(payload);
            JsonNode ratesNode = root.path("rates");
            Map<String, BigDecimal> result = new HashMap<>();
            if (ratesNode.isObject()) {
                ratesNode.fields().forEachRemaining(entry -> {
                    BigDecimal value = entry.getValue().decimalValue();
                    result.put(normalizeCurrency(entry.getKey()), value);
                });
            }
            result.put(base, BigDecimal.ONE);
            properties.getSupportedCurrencies().stream()
                    .map(this::normalizeCurrency)
                    .filter(code -> !result.containsKey(code) && !code.equalsIgnoreCase(base))
                    .forEach(code -> result.put(code, BigDecimal.ONE));
            return result;
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to fetch exchange rates", ex);
        }
    }

    private String normalizeCurrency(String code) {
        if (code == null || code.isBlank()) {
            return properties.getBaseCurrency();
        }
        return code.trim().toUpperCase();
    }

    private String resolvePreferredCurrency(String authToken, String customerId) {
        if (authToken == null || authToken.isBlank()) {
            return properties.getBaseCurrency();
        }
        try {
            Map<String, Object> profile = customerServiceClient.getCurrentProfile(authToken);
            Object preferred = profile != null ? profile.get("preferredCurrency") : null;
            if (preferred != null) {
                String code = preferred.toString().trim().toUpperCase();
                if (!code.isEmpty()) {
                    return code;
                }
            }
        } catch (Exception ignored) {
        }
        return properties.getBaseCurrency();
    }

    private record RateSnapshot(Map<String, BigDecimal> rates, LocalDateTime fetchedAt) {
    }
}
