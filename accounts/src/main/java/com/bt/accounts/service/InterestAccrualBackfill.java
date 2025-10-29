package com.bt.accounts.service;

import com.bt.accounts.entity.AccountTransaction;
import com.bt.accounts.entity.FdAccount;
import com.bt.accounts.repository.AccountTransactionRepository;
import com.bt.accounts.repository.FdAccountRepository;
import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class InterestAccrualBackfill {

    private final FdAccountRepository accountRepository;
    private final AccountTransactionRepository transactionRepository;

    @PostConstruct
    @Transactional
    public void backfill() {
        List<FdAccount> accounts = accountRepository.findAll();
        for (FdAccount account : accounts) {
            boolean updated = false;
            List<AccountTransaction> interestTransactions = transactionRepository
                    .findByAccountNoAndTransactionTypeOrderByTransactionDateDesc(
                            account.getAccountNo(), AccountTransaction.TransactionType.INTEREST_CREDIT);
            BigDecimal totalInterest = interestTransactions.stream()
                    .map(AccountTransaction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            LocalDateTime lastAccrual = interestTransactions.isEmpty()
                    ? null
                    : interestTransactions.get(0).getTransactionDate();
            LocalDateTime nextAccrual = resolveNextAccrual(account, lastAccrual);

            if (!Objects.equals(account.getLastInterestAccrualAt(), lastAccrual)) {
                account.setLastInterestAccrualAt(lastAccrual);
                updated = true;
            }
            if ((account.getTotalInterestAccrued() == null && totalInterest.compareTo(BigDecimal.ZERO) != 0)
                    || (account.getTotalInterestAccrued() != null
                    && account.getTotalInterestAccrued().compareTo(totalInterest) != 0)) {
                account.setTotalInterestAccrued(totalInterest);
                updated = true;
            }
            if (!Objects.equals(account.getNextInterestAccrualAt(), nextAccrual)) {
                account.setNextInterestAccrualAt(nextAccrual);
                updated = true;
            }
            if (account.getProductMaxTenureMonths() == null) {
                account.setProductMaxTenureMonths(account.getTenureMonths());
                updated = true;
            }
            if (updated) {
                accountRepository.save(account);
                log.info("Backfilled interest metadata for account {}", account.getAccountNo());
            }
        }
    }

    private LocalDateTime resolveNextAccrual(FdAccount account, LocalDateTime lastAccrual) {
        if (account.getStatus() != FdAccount.AccountStatus.ACTIVE) {
            return null;
        }
        if (lastAccrual != null) {
            return lastAccrual.plusYears(1);
        }
        LocalDateTime createdAt = account.getCreatedAt();
        if (createdAt != null) {
            return createdAt.plusYears(1);
        }
        return null;
    }
}
