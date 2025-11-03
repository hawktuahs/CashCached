package com.bt.accounts.repository;

import com.bt.accounts.entity.FdAccount;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class FdAccountRepositoryTest {

    @Autowired
    private FdAccountRepository accountRepository;

    private FdAccount testAccount;

    @BeforeEach
    void setUp() {
        testAccount = FdAccount.builder()
                .accountNo("FD-BR001-20251023-10000001")
                .customerId("CUST001")
                .productCode("FD-PREMIUM")
                .principalAmount(new BigDecimal("100000.00"))
                .interestRate(new BigDecimal("6.75"))
                .tenureMonths(24)
                .branchCode("BR001")
                .status(FdAccount.AccountStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();

        accountRepository.save(testAccount);
    }

    @Test
    void findByAccountNo_WithExistingAccount_ShouldReturnAccount() {
        Optional<FdAccount> found = accountRepository.findByAccountNo("FD-BR001-20251023-10000001");

        assertTrue(found.isPresent());
        assertEquals("CUST001", found.get().getCustomerId());
        assertEquals("FD-PREMIUM", found.get().getProductCode());
    }

    @Test
    void findByAccountNo_WithNonExistingAccount_ShouldReturnEmpty() {
        Optional<FdAccount> found = accountRepository.findByAccountNo("INVALID");

        assertFalse(found.isPresent());
    }

    @Test
    void findByCustomerId_WithExistingCustomer_ShouldReturnAccounts() {
        List<FdAccount> accounts = accountRepository.findByCustomerId("CUST001");

        assertFalse(accounts.isEmpty());
        assertEquals(1, accounts.size());
        assertEquals("FD-BR001-20251023-10000001", accounts.get(0).getAccountNo());
    }

    @Test
    void findByStatus_WithActiveStatus_ShouldReturnActiveAccounts() {
        List<FdAccount> accounts = accountRepository.findByStatus(FdAccount.AccountStatus.ACTIVE);

        assertFalse(accounts.isEmpty());
        accounts.forEach(account -> assertEquals(FdAccount.AccountStatus.ACTIVE, account.getStatus()));
    }

    @Test
    void existsByAccountNo_WithExistingAccount_ShouldReturnTrue() {
        boolean exists = accountRepository.existsByAccountNo("FD-BR001-20251023-10000001");

        assertTrue(exists);
    }

    @Test
    void existsByAccountNo_WithNonExistingAccount_ShouldReturnFalse() {
        boolean exists = accountRepository.existsByAccountNo("INVALID");

        assertFalse(exists);
    }

    @Test
    void countByStatus_WithActiveStatus_ShouldReturnCount() {
        Long count = accountRepository.countByStatus(FdAccount.AccountStatus.ACTIVE);

        assertNotNull(count);
        assertTrue(count >= 1);
    }
}
