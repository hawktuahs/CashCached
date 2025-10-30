package com.bt.accounts.service;

import com.bt.accounts.entity.FdAccount.AccountStatus;
import com.bt.accounts.entity.FdAccount;
import com.bt.accounts.repository.FdAccountRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.StringWriter;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AccountReportService {

    private final FdAccountRepository fdAccountRepository;

    @Transactional(readOnly = true)
    public String generateAccountReportCsv(
            String customerId,
            String productCode,
            AccountStatus status,
            LocalDate createdDateFrom,
            LocalDate createdDateTo,
            LocalDate maturityDateFrom,
            LocalDate maturityDateTo) {

        List<FdAccount> accounts = getFilteredAccounts(
                customerId, productCode, status,
                createdDateFrom, createdDateTo,
                maturityDateFrom, maturityDateTo);

        return convertAccountsToCsv(accounts);
    }

    private List<FdAccount> getFilteredAccounts(
            String customerId,
            String productCode,
            AccountStatus status,
            LocalDate createdDateFrom,
            LocalDate createdDateTo,
            LocalDate maturityDateFrom,
            LocalDate maturityDateTo) {

        List<FdAccount> accounts = fdAccountRepository.findAll();

        return accounts.stream()
                .filter(a -> customerId == null || a.getCustomerId().equals(customerId))
                .filter(a -> productCode == null || a.getProductCode().equals(productCode))
                .filter(a -> status == null || a.getStatus() == status)
                .filter(a -> createdDateFrom == null || a.getCreatedAt().toLocalDate().isAfter(createdDateFrom)
                        || a.getCreatedAt().toLocalDate().isEqual(createdDateFrom))
                .filter(a -> createdDateTo == null || a.getCreatedAt().toLocalDate().isBefore(createdDateTo)
                        || a.getCreatedAt().toLocalDate().isEqual(createdDateTo))
                .filter(a -> maturityDateFrom == null
                        || a.getMaturityDate() != null && (a.getMaturityDate().toLocalDate().isAfter(maturityDateFrom)
                                || a.getMaturityDate().toLocalDate().isEqual(maturityDateFrom)))
                .filter(a -> maturityDateTo == null
                        || a.getMaturityDate() != null && (a.getMaturityDate().toLocalDate().isBefore(maturityDateTo)
                                || a.getMaturityDate().toLocalDate().isEqual(maturityDateTo)))
                .collect(Collectors.toList());
    }

    private String convertAccountsToCsv(List<FdAccount> accounts) {
        StringWriter stringWriter = new StringWriter();
        String[] headers = { "Account ID", "Account No", "Customer ID", "Product Code", "Principal Amount",
                "Interest Rate", "Base Interest Rate", "Tenure (Months)", "Maturity Amount", "Maturity Date",
                "Next Payout At", "Branch Code", "Status", "Created At", "Created By", "Closed At", "Closed By",
                "Closure Reason", "Updated At", "Last Interest Accrual At", "Next Interest Accrual At",
                "Total Interest Accrued", "Pricing Rule ID", "Pricing Rule Name" };

        try (CSVPrinter csvPrinter = new CSVPrinter(stringWriter, CSVFormat.DEFAULT)) {
            csvPrinter.printRecord((Object[]) headers);

            for (FdAccount account : accounts) {
                csvPrinter.printRecord(
                        account.getId(),
                        account.getAccountNo(),
                        account.getCustomerId(),
                        account.getProductCode(),
                        account.getPrincipalAmount(),
                        account.getInterestRate(),
                        account.getBaseInterestRate(),
                        account.getTenureMonths(),
                        account.getMaturityAmount(),
                        account.getMaturityDate(),
                        account.getNextPayoutAt(),
                        account.getBranchCode(),
                        account.getStatus(),
                        account.getCreatedAt(),
                        account.getCreatedBy(),
                        account.getClosedAt(),
                        account.getClosedBy(),
                        account.getClosureReason(),
                        account.getUpdatedAt(),
                        account.getLastInterestAccrualAt(),
                        account.getNextInterestAccrualAt(),
                        account.getTotalInterestAccrued(),
                        account.getActivePricingRuleId(),
                        account.getActivePricingRuleName());
            }

            return stringWriter.toString();
        } catch (IOException e) {
            throw new RuntimeException("Error generating CSV report", e);
        }
    }
}
