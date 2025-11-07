package com.bt.accounts.scheduler;

import com.bt.accounts.dto.TransactionRequest;
import com.bt.accounts.dto.TransactionResponse;
import com.bt.accounts.entity.AccountTransaction;
import com.bt.accounts.entity.FdAccount;
import com.bt.accounts.service.TransactionService;
import com.bt.accounts.service.CashCachedService;
import com.bt.accounts.service.PricingRuleEvaluator;
import com.bt.accounts.service.ServiceTokenProvider;
import com.bt.accounts.exception.ServiceIntegrationException;
import com.bt.accounts.exception.AccountNotFoundException;
import com.bt.accounts.repository.AccountTransactionRepository;
import com.bt.accounts.repository.FdAccountRepository;
import com.bt.accounts.time.TimeProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.math.RoundingMode;

@Component
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class AccrualScheduler {

    private final FdAccountRepository accountRepository;
    private final AccountTransactionRepository txnRepository;
    private final TransactionService transactionService;
    private final CashCachedService cashCachedService;
    private final PricingRuleEvaluator pricingRuleEvaluator;
    private final ServiceTokenProvider serviceTokenProvider;
    private final TimeProvider timeProvider;

    @Scheduled(fixedDelay = 60_000)
    public void runAccruals() {
        Instant nowInstant = timeProvider.now();
        ZoneId zone = ZoneId.systemDefault();
        LocalDateTime now = LocalDateTime.ofInstant(nowInstant, zone);
        List<FdAccount> accounts = accountRepository.findAll()
                .stream()
                .filter(a -> a.getStatus() == FdAccount.AccountStatus.ACTIVE)
                .collect(Collectors.toList());

        for (FdAccount a : accounts) {
            try {
                processAccountAccrual(a, now);
            } catch (Exception ex) {
                log.warn("Accrual failed for account {}: {}", a.getAccountNo(), ex.getMessage());
            }

        }
    }

    private PricingRuleEvaluator.EvaluationResult evaluatePricing(FdAccount account, BigDecimal balance) {
        try {
            String token = serviceTokenProvider.getBearerToken();
            return pricingRuleEvaluator.evaluate(account, balance, token);
        } catch (ServiceIntegrationException ex) {
            log.warn("Skipping pricing evaluation for account {}: {}", account.getAccountNo(), ex.getMessage());
            return PricingRuleEvaluator.EvaluationResult.noRule(account.getBaseInterestRate());
        }
    }

    private BigDecimal resolveAppliedRate(FdAccount account, PricingRuleEvaluator.EvaluationResult pricing) {
        BigDecimal base = account.getBaseInterestRate() != null ? account.getBaseInterestRate()
                : account.getInterestRate();
        BigDecimal applied = pricing.getAppliedRate() != null ? pricing.getAppliedRate() : base;
        return applied != null ? applied.setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    private boolean shouldUpdatePricingMetadata(FdAccount account, PricingRuleEvaluator.EvaluationResult pricing,
            BigDecimal appliedRate) {
        boolean rateChanged = account.getInterestRate() == null
                || account.getInterestRate().compareTo(appliedRate) != 0;
        Long ruleId = pricing.getRule() != null ? pricing.getRule().getId() : null;
        String ruleName = pricing.getRule() != null ? pricing.getRule().getRuleName() : null;
        boolean idChanged = !Objects.equals(account.getActivePricingRuleId(), ruleId);
        boolean nameChanged = !Objects.equals(account.getActivePricingRuleName(), ruleName);
        boolean appliedAtChanged = pricing.getRule() != null ? account.getPricingRuleAppliedAt() == null
                : account.getPricingRuleAppliedAt() != null;
        return rateChanged || idChanged || nameChanged || appliedAtChanged;
    }

    private void processAccountAccrual(FdAccount account, LocalDateTime now) {
        if (account.getStatus() != FdAccount.AccountStatus.ACTIVE) {
            return;
        }

        String accountNo = account.getAccountNo();
        BigDecimal totalAccrued = account.getTotalInterestAccrued() != null ? account.getTotalInterestAccrued()
                : BigDecimal.ZERO;
        LocalDateTime nextAccrual = resolveNextAccrual(account);
        LocalDateTime createdAt = account.getCreatedAt() != null ? account.getCreatedAt() : now;

        boolean metadataUpdated = false;

        while (nextAccrual != null && !nextAccrual.isAfter(now)
                && account.getStatus() == FdAccount.AccountStatus.ACTIVE) {
            BigDecimal currentBalance = calculateCurrentBalance(accountNo);
            PricingRuleEvaluator.EvaluationResult pricing = evaluatePricing(account, currentBalance);
            BigDecimal appliedRate = resolveAppliedRate(account, pricing);
            if (shouldUpdatePricingMetadata(account, pricing, appliedRate)) {
                account.setInterestRate(appliedRate);
                account.setActivePricingRuleId(pricing.getRule() != null ? pricing.getRule().getId() : null);
                account.setActivePricingRuleName(pricing.getRule() != null ? pricing.getRule().getRuleName() : null);
                account.setPricingRuleAppliedAt(pricing.getRule() != null ? now : null);
                metadataUpdated = true;
            }
            BigDecimal annualRate = account.getInterestRate() != null ? account.getInterestRate() : BigDecimal.ZERO;
            BigDecimal annualRateDecimal = annualRate.divide(BigDecimal.valueOf(100));
            BigDecimal interestRaw = currentBalance.multiply(annualRateDecimal);
            BigDecimal interest = interestRaw.setScale(0, RoundingMode.CEILING);

            if (interest.compareTo(BigDecimal.ONE) >= 0) {
                mintInterestTokens(account, interest, nextAccrual);
                TransactionResponse txn = creditInterest(account, interest, nextAccrual);
                currentBalance = txn.getBalanceAfter();
                totalAccrued = totalAccrued.add(interest);
                log.info("Accrued {} tokens interest for account {} on {}", interest, accountNo,
                        nextAccrual.toLocalDate());
            } else {
                log.debug("Accrual skipped for account={} on {} due to interest {} < 1", accountNo,
                        nextAccrual.toLocalDate(), interest);
            }

            account.setLastInterestAccrualAt(nextAccrual);
            nextAccrual = nextAccrual.plusYears(1);
            account.setNextInterestAccrualAt(nextAccrual);
            account.setTotalInterestAccrued(totalAccrued);
            metadataUpdated = true;

            if (isAtMaturity(account, nextAccrual)) {
                break;
            }
        }

        if (metadataUpdated) {
            accountRepository.save(account);
        }

        if (shouldFinalizeMaturity(account, createdAt, now)) {
            BigDecimal currentBalance = calculateCurrentBalance(accountNo);
            finalizeMaturity(account, currentBalance, now);
        }
    }

    private LocalDateTime resolveNextAccrual(FdAccount account) {
        LocalDateTime next = account.getNextInterestAccrualAt();
        if (next != null) {
            return next;
        }
        LocalDateTime base = account.getLastInterestAccrualAt();
        if (base != null) {
            next = base.plusYears(1);
        } else if (account.getCreatedAt() != null) {
            next = account.getCreatedAt().plusYears(1);
        }
        if (next == null) {
            next = LocalDateTime.now().plusYears(1);
        }
        account.setNextInterestAccrualAt(next);
        if (account.getTotalInterestAccrued() == null) {
            account.setTotalInterestAccrued(BigDecimal.ZERO);
        }
        return next;
    }

    private BigDecimal calculateCurrentBalance(String accountNo) {
        List<AccountTransaction> txns = txnRepository.findByAccountNoOrderByTransactionDateDesc(accountNo);
        if (txns.isEmpty()) {
            FdAccount account = accountRepository.findByAccountNo(accountNo)
                    .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountNo));
            return account.getPrincipalAmount();
        }
        return txns.get(0).getBalanceAfter();
    }

    private TransactionResponse creditInterest(FdAccount account, BigDecimal amount, LocalDateTime when) {
        return transactionService.recordTransaction(account.getAccountNo(), TransactionRequest.builder()
                .transactionType(AccountTransaction.TransactionType.INTEREST_CREDIT.name())
                .amount(amount)
                .description("Annual interest credit")
                .remarks("System accrual")
                .build(), when);
    }

    private void mintInterestTokens(FdAccount account, BigDecimal amount, LocalDateTime when) {
        String reference = "Interest accrual " + account.getAccountNo() + " @ " + when.toLocalDate();
        try {
            cashCachedService.mintForInterest(amount, reference);
            log.info("Minted {} CCHD interest backing for account {}", amount, account.getAccountNo());
        } catch (Exception ex) {
            log.error("Minting interest failed for account {}: {}", account.getAccountNo(), ex.getMessage(), ex);
            throw ex;
        }
    }

    private void finalizeMaturity(FdAccount account, BigDecimal balance, LocalDateTime when) {
        BigDecimal payout = balance.setScale(0, RoundingMode.DOWN);
        if (payout.compareTo(BigDecimal.ONE) < 0) {
            markAccountClosed(account, when);
            return;
        }
        transactionService.recordTransaction(account.getAccountNo(), TransactionRequest.builder()
                .transactionType(AccountTransaction.TransactionType.MATURITY_PAYOUT.name())
                .amount(payout)
                .description("Maturity payout to wallet")
                .remarks("System maturity closure")
                .build(), when);
        markAccountClosed(account, when);
        log.info("Account {} matured and paid {} tokens", account.getAccountNo(), balance);
    }

    private void markAccountClosed(FdAccount account, LocalDateTime when) {
        account.setStatus(FdAccount.AccountStatus.MATURED);
        account.setClosedAt(when);
        account.setClosedBy("system");
        account.setNextInterestAccrualAt(null);
        account.setNextPayoutAt(null);
        accountRepository.save(account);
    }

    private boolean shouldFinalizeMaturity(FdAccount account, LocalDateTime createdAt, LocalDateTime now) {
        LocalDateTime payoutAt = account.getNextPayoutAt();
        if (payoutAt == null) {
            Integer tenure = account.getTenureMonths();
            Integer productMax = account.getProductMaxTenureMonths();
            int effectiveTenure = tenure != null ? tenure : (productMax != null ? productMax : 0);
            if (effectiveTenure > 0 && createdAt != null) {
                payoutAt = createdAt.plusMonths(effectiveTenure);
            }
        }
        return payoutAt != null && !payoutAt.isAfter(now) && account.getStatus() == FdAccount.AccountStatus.ACTIVE;
    }

    private boolean isAtMaturity(FdAccount account, LocalDateTime nextAccrual) {
        LocalDateTime payoutAt = account.getNextPayoutAt();
        if (payoutAt == null) {
            Integer tenure = account.getTenureMonths();
            Integer productMax = account.getProductMaxTenureMonths();
            int effectiveTenure = tenure != null ? tenure : (productMax != null ? productMax : 0);
            LocalDateTime createdAt = account.getCreatedAt();
            if (effectiveTenure > 0 && createdAt != null) {
                payoutAt = createdAt.plusMonths(effectiveTenure);
            }
        }
        return payoutAt != null && !nextAccrual.isBefore(payoutAt);
    }
}
