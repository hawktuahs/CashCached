package com.bt.accounts.service;

import com.bt.accounts.repository.FdAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountNumberGenerator {

    private final FdAccountRepository accountRepository;
    private final AtomicLong sequenceCounter = new AtomicLong(10000001);

    @Value("${accounts.iban.country-code:IN}")
    private String countryCode;

    @Value("${accounts.iban.bank-code:SBIN}")
    private String bankCode;

    @Value("${accounts.iban.enabled:true}")
    private boolean ibanEnabled;

    @Value("${accounts.sequence.prefix:FD}")
    private String accountPrefix;

    public synchronized String generateAccountNumber(String branchCode) {
        int attempts = 0;
        while (attempts < 25) {
            String candidate = ibanEnabled ? generateIBAN(branchCode) : generateLegacyAccountNumber(branchCode);
            if (!accountRepository.existsByAccountNo(candidate)) {
                return candidate;
            }
            attempts++;
            log.warn("Generated duplicate account number {} for branch {}. Retrying (attempt {})", candidate,
                    branchCode, attempts);
        }
        throw new IllegalStateException("Unable to generate unique account number after multiple attempts for branch "
                + branchCode);
    }

    public String generateIBAN(String branchCode) {
        String baseAccountNumber = generateBaseAccountNumber(branchCode);
        String normalizedBranch = normalizeBranchCode(branchCode);
        String bban = String.format("%s%s%s", bankCode, normalizedBranch, baseAccountNumber);
        String checkDigits = calculateIBANCheckDigits(bban);
        String iban = String.format("%s%s%s", countryCode, checkDigits, bban);
        log.debug("Generated IBAN: {} (length: {}) for branch: {}", iban, iban.length(), branchCode);
        return iban;
    }

    public String generateLegacyAccountNumber(String branchCode) {
        String dateComponent = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        Long todayCount = accountRepository.countTodayAccountsByBranch(branchCode);
        long sequence = sequenceCounter.incrementAndGet() + todayCount;
        return String.format("%s-%s-%s-%08d", accountPrefix, branchCode, dateComponent, sequence);
    }

    private String generateBaseAccountNumber(String branchCode) {
        Long todayCount = accountRepository.countTodayAccountsByBranch(branchCode);
        long sequence = sequenceCounter.incrementAndGet() + todayCount;
        String accountNumberWithoutCheck = String.format("%019d", sequence);
        int checkDigit = calculateLuhnCheckDigit(accountNumberWithoutCheck);
        return accountNumberWithoutCheck + checkDigit;
    }

    private String calculateIBANCheckDigits(String bban) {
        String rearranged = bban + countryCode + "00";
        StringBuilder numericString = new StringBuilder();
        for (char ch : rearranged.toCharArray()) {
            if (Character.isDigit(ch)) {
                numericString.append(ch);
            } else if (Character.isLetter(ch)) {
                numericString.append(Character.toUpperCase(ch) - 'A' + 10);
            }
        }
        BigInteger numericValue = new BigInteger(numericString.toString());
        int remainder = numericValue.mod(BigInteger.valueOf(97)).intValue();
        int checkDigit = 98 - remainder;
        return String.format("%02d", checkDigit);
    }

    private int calculateLuhnCheckDigit(String number) {
        int sum = 0;
        boolean alternate = true;
        for (int i = number.length() - 1; i >= 0; i--) {
            int digit = Character.getNumericValue(number.charAt(i));
            if (alternate) {
                digit *= 2;
                if (digit > 9) {
                    digit = (digit % 10) + 1;
                }
            }
            sum += digit;
            alternate = !alternate;
        }
        return (10 - (sum % 10)) % 10;
    }

    private String normalizeBranchCode(String branchCode) {
        String cleaned = branchCode.replaceAll("[^A-Z0-9]", "");
        if (cleaned.length() < 6) {
            return String.format("%-6s", cleaned).replace(' ', '0');
        } else {
            return cleaned.substring(0, 6);
        }
    }

    public boolean validateIBAN(String iban) {
        if (iban == null || iban.length() != 34) {
            log.debug("IBAN validation failed - invalid length: {}", iban != null ? iban.length() : "null");
            return false;
        }
        try {
            String checkDigits = iban.substring(2, 4);
            String bban = iban.substring(4);
            String calculatedCheckDigits = calculateIBANCheckDigits(bban);
            boolean isValid = checkDigits.equals(calculatedCheckDigits);
            log.debug("IBAN validation - Input: {}, Expected: {}, Valid: {}",
                    checkDigits, calculatedCheckDigits, isValid);
            return isValid;
        } catch (Exception e) {
            log.error("IBAN validation error: {}", e.getMessage());
            return false;
        }
    }

    public boolean validateLuhnCheckDigit(String accountNumber) {
        if (accountNumber == null || accountNumber.length() < 2) {
            return false;
        }
        try {
            String numberPart = accountNumber.substring(0, accountNumber.length() - 1);
            int providedCheckDigit = Character.getNumericValue(
                    accountNumber.charAt(accountNumber.length() - 1));
            int calculatedCheckDigit = calculateLuhnCheckDigit(numberPart);
            return providedCheckDigit == calculatedCheckDigit;
        } catch (Exception e) {
            log.error("Luhn validation error: {}", e.getMessage());
            return false;
        }
    }

    public IBANComponents parseIBAN(String iban) {
        if (iban == null || iban.length() != 34) {
            throw new IllegalArgumentException("Invalid IBAN format - India IBANs must be 34 characters");
        }
        return IBANComponents.builder()
                .countryCode(iban.substring(0, 2))
                .checkDigits(iban.substring(2, 4))
                .bankCode(iban.substring(4, 8))
                .branchCode(iban.substring(8, 14))
                .accountNumber(iban.substring(14))
                .fullIBAN(iban)
                .build();
    }

    @lombok.Builder
    @lombok.Getter
    @lombok.AllArgsConstructor
    public static class IBANComponents {
        private String countryCode;
        private String checkDigits;
        private String bankCode;
        private String branchCode;
        private String accountNumber;
        private String fullIBAN;

        @Override
        public String toString() {
            return String.format("IBAN[Country=%s, Check=%s, Bank=%s, Branch=%s, Account=%s]",
                    countryCode, checkDigits, bankCode, branchCode, accountNumber);
        }
    }
}
