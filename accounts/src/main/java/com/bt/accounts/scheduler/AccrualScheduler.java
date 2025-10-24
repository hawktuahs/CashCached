package com.bt.accounts.scheduler;

import com.bt.accounts.entity.AccountTransaction;
import com.bt.accounts.entity.FdAccount;
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
import java.util.stream.Collectors;

@Component
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class AccrualScheduler {

    private final FdAccountRepository accountRepository;
    private final AccountTransactionRepository txnRepository;
    private final TimeProvider timeProvider;

    private static final int INTERVAL_MONTHS = 3; // Global compounding interval

    @Scheduled(fixedDelay = 60_000)
    public void runAccruals() {
        Instant nowInstant = timeProvider.now();
        LocalDateTime now = LocalDateTime.ofInstant(nowInstant, ZoneId.systemDefault());
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

    private void processAccountAccrual(FdAccount account, LocalDateTime now) {
        String accountNo = account.getAccountNo();
        LocalDateTime openedAt = account.getCreatedAt();
        if (openedAt == null) openedAt = now;

        // Count existing interest credits for this account
        List<AccountTransaction> txns = txnRepository.findByAccountNo(accountNo);
        long creditedIntervals = txns.stream()
                .filter(t -> t.getTransactionType() == AccountTransaction.TransactionType.INTEREST_CREDIT)
                .count();

        // Determine intervals elapsed since open
        Period p = Period.between(openedAt.toLocalDate(), now.toLocalDate());
        int monthsElapsed = p.getYears() * 12 + p.getMonths();
        long intervalsElapsed = monthsElapsed / INTERVAL_MONTHS;

        long due = intervalsElapsed - creditedIntervals;
        if (due <= 0) return;

        BigDecimal annualRate = account.getInterestRate() != null ? account.getInterestRate() : BigDecimal.ZERO;
        BigDecimal principal = account.getPrincipalAmount() != null ? account.getPrincipalAmount() : BigDecimal.ZERO;
        BigDecimal intervalRate = annualRate
                .divide(BigDecimal.valueOf(100))
                .multiply(BigDecimal.valueOf(INTERVAL_MONTHS))
                .divide(BigDecimal.valueOf(12), BigDecimal.ROUND_HALF_UP);

        for (int i = 0; i < due; i++) {
            BigDecimal currentBalance = calculateCurrentBalance(accountNo);
            // Interest credited on current balance; adjust if you want principal-only
            BigDecimal interest = currentBalance.multiply(intervalRate);
            creditInterest(accountNo, interest, now);
        }
    }

    private BigDecimal calculateCurrentBalance(String accountNo) {
        List<AccountTransaction> txns = txnRepository.findByAccountNo(accountNo);
        if (txns.isEmpty()) {
            FdAccount account = accountRepository.findByAccountNo(accountNo)
                    .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountNo));
            return account.getPrincipalAmount();
        }
        return txns.get(txns.size() - 1).getBalanceAfter();
    }

    private void creditInterest(String accountNo, BigDecimal amount, LocalDateTime when) {
        AccountTransaction t = AccountTransaction.builder()
                .transactionId(generateTxnId(accountNo))
                .accountNo(accountNo)
                .transactionType(AccountTransaction.TransactionType.INTEREST_CREDIT)
                .amount(amount)
                .balanceAfter(calculateCurrentBalance(accountNo).add(amount))
                .description("Periodic interest credit")
                .processedBy("system")
                .transactionDate(when)
                .build();
        txnRepository.save(t);
        log.info("Credited interest {} to {}", amount, accountNo);
    }

    private String generateTxnId(String accountNo) {
        return "TXN-INT-" + accountNo + "-" + timeProvider.now().toEpochMilli();
    }
}
