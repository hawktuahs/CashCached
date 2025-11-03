package com.bt.fixeddeposit.repository;

import com.bt.fixeddeposit.entity.FdCalculation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class FdCalculationRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private FdCalculationRepository calculationRepository;

    private FdCalculation calculation1;
    private FdCalculation calculation2;

    @BeforeEach
    void setUp() {
        calculation1 = FdCalculation.builder()
                .customerId(1L)
                .productCode("FD-001")
                .principalAmount(BigDecimal.valueOf(100000))
                .tenureMonths(12)
                .interestRate(BigDecimal.valueOf(6.5))
                .compoundingFrequency(4)
                .maturityAmount(BigDecimal.valueOf(106659.46))
                .interestEarned(BigDecimal.valueOf(6659.46))
                .effectiveRate(BigDecimal.valueOf(6.66))
                .currency("USD")
                .calculationDate(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();

        calculation2 = FdCalculation.builder()
                .customerId(1L)
                .productCode("FD-002")
                .principalAmount(BigDecimal.valueOf(200000))
                .tenureMonths(24)
                .interestRate(BigDecimal.valueOf(7.0))
                .compoundingFrequency(4)
                .maturityAmount(BigDecimal.valueOf(229953.39))
                .interestEarned(BigDecimal.valueOf(29953.39))
                .effectiveRate(BigDecimal.valueOf(7.19))
                .currency("USD")
                .calculationDate(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void testSaveAndFindById() {
        FdCalculation saved = entityManager.persistAndFlush(calculation1);

        FdCalculation found = calculationRepository.findById(saved.getId()).orElse(null);

        assertNotNull(found);
        assertEquals(saved.getId(), found.getId());
        assertEquals(BigDecimal.valueOf(100000), found.getPrincipalAmount());
        assertEquals("FD-001", found.getProductCode());
    }

    @Test
    void testFindByCustomerIdOrderByCreatedAtDesc() {
        entityManager.persist(calculation1);
        entityManager.persist(calculation2);
        entityManager.flush();

        List<FdCalculation> calculations = calculationRepository.findByCustomerIdOrderByCreatedAtDesc(1L);

        assertNotNull(calculations);
        assertEquals(2, calculations.size());
        assertEquals(1L, calculations.get(0).getCustomerId());
    }

    @Test
    void testFindByCustomerIdAndProductCodeOrderByCreatedAtDesc() {
        entityManager.persist(calculation1);
        entityManager.persist(calculation2);
        entityManager.flush();

        List<FdCalculation> calculations = calculationRepository
                .findByCustomerIdAndProductCodeOrderByCreatedAtDesc(1L, "FD-001");

        assertNotNull(calculations);
        assertEquals(1, calculations.size());
        assertEquals("FD-001", calculations.get(0).getProductCode());
    }

    @Test
    void testFindRecentCalculationsByCustomer() {
        entityManager.persist(calculation1);
        entityManager.persist(calculation2);
        entityManager.flush();

        LocalDateTime startDate = LocalDateTime.now().minusDays(1);
        List<FdCalculation> calculations = calculationRepository
                .findRecentCalculationsByCustomer(1L, startDate);

        assertNotNull(calculations);
        assertEquals(2, calculations.size());
    }

    @Test
    void testFindCalculationsByProductAndDate() {
        entityManager.persist(calculation1);
        entityManager.persist(calculation2);
        entityManager.flush();

        LocalDateTime startDate = LocalDateTime.now().minusDays(1);
        List<FdCalculation> calculations = calculationRepository
                .findCalculationsByProductAndDate("FD-001", startDate);

        assertNotNull(calculations);
        assertEquals(1, calculations.size());
        assertEquals("FD-001", calculations.get(0).getProductCode());
    }

    @Test
    void testCountByCustomerId() {
        entityManager.persist(calculation1);
        entityManager.persist(calculation2);
        entityManager.flush();

        Long count = calculationRepository.countByCustomerId(1L);

        assertNotNull(count);
        assertEquals(2, count);
    }

    @Test
    void testCountByCustomerId_NoResults() {
        Long count = calculationRepository.countByCustomerId(999L);

        assertNotNull(count);
        assertEquals(0, count);
    }
}
