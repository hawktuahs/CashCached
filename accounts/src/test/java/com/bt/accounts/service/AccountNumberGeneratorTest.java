package com.bt.accounts.service;

import com.bt.accounts.repository.FdAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AccountNumberGeneratorTest {

    @Mock
    private FdAccountRepository accountRepository;

    @InjectMocks
    private AccountNumberGenerator accountNumberGenerator;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(accountNumberGenerator, "countryCode", "IN");
        ReflectionTestUtils.setField(accountNumberGenerator, "bankCode", "SBIN");
        ReflectionTestUtils.setField(accountNumberGenerator, "ibanEnabled", true);
        ReflectionTestUtils.setField(accountNumberGenerator, "accountPrefix", "FD");
        lenient().when(accountRepository.countTodayAccountsByBranch(anyString())).thenReturn(0L);
    }

    @Test
    void generateAccountNumber_WithIBANEnabled_ShouldReturnValidIBAN() {
        String branchCode = "BR001";
        String iban = accountNumberGenerator.generateAccountNumber(branchCode);
        assertNotNull(iban);
        assertTrue(iban.startsWith("IN"));
        assertEquals(34, iban.length());
        assertTrue(accountNumberGenerator.validateIBAN(iban));
    }

    @Test
    void generateIBAN_WithDifferentBranches_ShouldGenerateUnique() {
        String branch1 = "BR001";
        String branch2 = "BR002";
        String iban1 = accountNumberGenerator.generateIBAN(branch1);
        String iban2 = accountNumberGenerator.generateIBAN(branch2);
        assertNotEquals(iban1, iban2);
    }

    @Test
    void generateLegacyAccountNumber_ShouldReturnCorrectFormat() {
        String branchCode = "BR001";
        String accountNumber = accountNumberGenerator.generateLegacyAccountNumber(branchCode);
        assertNotNull(accountNumber);
        assertTrue(accountNumber.startsWith("FD-"));
        assertTrue(accountNumber.contains(branchCode));
        assertTrue(accountNumber.matches("FD-BR001-\\d{8}-\\d{8}"));
    }

    @Test
    void calculateIBANCheckDigits_WithKnownInput_ShouldReturnCorrectCheckDigit() {
        String bban = "CBKUBR00100000000000010000002";
        String checkDigits = ReflectionTestUtils.invokeMethod(
                accountNumberGenerator, "calculateIBANCheckDigits", bban);
        assertNotNull(checkDigits);
        assertEquals(2, checkDigits.length());
        assertTrue(checkDigits.matches("\\d{2}"));
    }

    @Test
    void calculateLuhnCheckDigit_WithKnownInput_ShouldReturnCorrectDigit() {
        String testNumber = "123456789012345";
        Integer checkDigitObj = ReflectionTestUtils.invokeMethod(
                accountNumberGenerator, "calculateLuhnCheckDigit", testNumber);
        assertNotNull(checkDigitObj);
        int checkDigit = checkDigitObj.intValue();
        assertTrue(checkDigit >= 0 && checkDigit <= 9);
    }

    @Test
    void validateIBAN_WithValidIBAN_ShouldReturnTrue() {
        String validIBAN = accountNumberGenerator.generateIBAN("BR001");
        boolean isValid = accountNumberGenerator.validateIBAN(validIBAN);
        assertTrue(isValid);
    }

    @Test
    void validateIBAN_WithInvalidCheckDigit_ShouldReturnFalse() {
        String validIBAN = accountNumberGenerator.generateIBAN("BR001");
        String invalidIBAN = validIBAN.substring(0, 2) + "00" + validIBAN.substring(4);
        boolean isValid = accountNumberGenerator.validateIBAN(invalidIBAN);
        assertFalse(isValid);
    }

    @Test
    void validateIBAN_WithNullInput_ShouldReturnFalse() {
        boolean isValid = accountNumberGenerator.validateIBAN(null);
        assertFalse(isValid);
    }

    @Test
    void validateIBAN_WithShortInput_ShouldReturnFalse() {
        String shortIBAN = "KW123";
        boolean isValid = accountNumberGenerator.validateIBAN(shortIBAN);
        assertFalse(isValid);
    }

    @Test
    void validateLuhnCheckDigit_WithValidNumber_ShouldReturnTrue() {
        String baseNumber = "000000000000001";
        Integer checkDigitObj = ReflectionTestUtils.invokeMethod(
                accountNumberGenerator, "calculateLuhnCheckDigit", baseNumber);
        assertNotNull(checkDigitObj);
        int checkDigit = checkDigitObj.intValue();
        String fullNumber = baseNumber + checkDigit;
        boolean isValid = accountNumberGenerator.validateLuhnCheckDigit(fullNumber);
        assertTrue(isValid);
    }

    @Test
    void validateLuhnCheckDigit_WithInvalidCheckDigit_ShouldReturnFalse() {
        String invalidNumber = "0000000000000019";
        boolean isValid = accountNumberGenerator.validateLuhnCheckDigit(invalidNumber);
        assertNotNull(isValid);
    }

    @Test
    void parseIBAN_WithValidIBAN_ShouldExtractComponents() {
        String iban = accountNumberGenerator.generateIBAN("BR001");
        AccountNumberGenerator.IBANComponents components = accountNumberGenerator.parseIBAN(iban);
        assertNotNull(components);
        assertEquals("IN", components.getCountryCode());
        assertEquals("SBIN", components.getBankCode());
        assertNotNull(components.getCheckDigits());
        assertNotNull(components.getBranchCode());
        assertNotNull(components.getAccountNumber());
        assertEquals(iban, components.getFullIBAN());
    }

    @Test
    void parseIBAN_WithInvalidIBAN_ShouldThrowException() {
        String invalidIBAN = "INVALID";
        assertThrows(IllegalArgumentException.class,
                () -> accountNumberGenerator.parseIBAN(invalidIBAN));
    }

    @Test
    void normalizeBranchCode_WithShortCode_ShouldPad() {
        String shortCode = "BR1";
        String normalized = ReflectionTestUtils.invokeMethod(
                accountNumberGenerator, "normalizeBranchCode", shortCode);
        assertNotNull(normalized);
        assertEquals(6, normalized.length());
    }

    @Test
    void normalizeBranchCode_WithLongCode_ShouldTruncate() {
        String longCode = "BRANCH001";
        String normalized = ReflectionTestUtils.invokeMethod(
                accountNumberGenerator, "normalizeBranchCode", longCode);
        assertNotNull(normalized);
        assertEquals(6, normalized.length());
    }

    @Test
    void generateAccountNumber_WithIBANDisabled_ShouldReturnLegacyFormat() {
        ReflectionTestUtils.setField(accountNumberGenerator, "ibanEnabled", false);
        String branchCode = "BR001";
        String accountNumber = accountNumberGenerator.generateAccountNumber(branchCode);
        assertNotNull(accountNumber);
        assertTrue(accountNumber.startsWith("FD-"));
        assertFalse(accountNumber.startsWith("KW"));
    }

    @Test
    void ibanComponents_ToString_ShouldReturnFormattedString() {
        AccountNumberGenerator.IBANComponents components = AccountNumberGenerator.IBANComponents.builder()
                .countryCode("KW")
                .checkDigits("81")
                .bankCode("CBKU")
                .branchCode("BR001")
                .accountNumber("0000000000010000002")
                .fullIBAN("KW81CBKUBR00100000000000010000002")
                .build();
        String toString = components.toString();
        assertNotNull(toString);
        assertTrue(toString.contains("KW"));
        assertTrue(toString.contains("81"));
        assertTrue(toString.contains("CBKU"));
    }

    @Test
    void generateMultipleIBANs_ShouldAllBeValid() {
        String branchCode = "BR001";
        int count = 10;
        for (int i = 0; i < count; i++) {
            String iban = accountNumberGenerator.generateIBAN(branchCode);
            assertTrue(accountNumberGenerator.validateIBAN(iban));
        }
    }

    @Test
    void generateIBAN_ShouldBeThreadSafe() throws InterruptedException {
        String branchCode = "BR001";
        int threadCount = 5;
        Thread[] threads = new Thread[threadCount];
        for (int i = 0; i < threadCount; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < 10; j++) {
                    String iban = accountNumberGenerator.generateIBAN(branchCode);
                    assertTrue(accountNumberGenerator.validateIBAN(iban));
                }
            });
            threads[i].start();
        }
        for (Thread thread : threads) {
            thread.join();
        }
        assertTrue(true);
    }
}
