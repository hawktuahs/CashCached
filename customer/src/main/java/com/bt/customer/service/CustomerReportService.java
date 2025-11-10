package com.bt.customer.service;

import com.bt.customer.entity.User;
import com.bt.customer.repository.UserRepository;
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
public class CustomerReportService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public String generateCustomerReportCsv(
            User.Role role,
            Boolean active,
            LocalDate createdDateFrom,
            LocalDate createdDateTo) {

        List<User> customers = getFilteredCustomers(role, active, createdDateFrom, createdDateTo);

        return convertCustomersToCsv(customers);
    }

    private List<User> getFilteredCustomers(
            User.Role role,
            Boolean active,
            LocalDate createdDateFrom,
            LocalDate createdDateTo) {

        List<User> customers = userRepository.findAll();

        return customers.stream()
                .filter(u -> role == null || u.getRole() == role)
                .filter(u -> active == null || u.getActive().equals(active))
                .filter(u -> createdDateFrom == null || u.getCreatedAt().toLocalDate().isAfter(createdDateFrom)
                        || u.getCreatedAt().toLocalDate().isEqual(createdDateFrom))
                .filter(u -> createdDateTo == null || u.getCreatedAt().toLocalDate().isBefore(createdDateTo)
                        || u.getCreatedAt().toLocalDate().isEqual(createdDateTo))
                .collect(Collectors.toList());
    }

    private String convertCustomersToCsv(List<User> customers) {
        StringWriter stringWriter = new StringWriter();
        String[] headers = { "ID", "Username", "Full Name", "Email", "Phone Number", "Role",
                "Preferred Currency", "Active", "Created At", "Updated At" };

        try (CSVPrinter csvPrinter = new CSVPrinter(stringWriter, CSVFormat.DEFAULT)) {
            csvPrinter.printRecord((Object[]) headers);

            for (User customer : customers) {
                csvPrinter.printRecord(
                        customer.getId(),
                        customer.getFullName(),
                        customer.getEmail(),
                        customer.getPhoneNumber(),
                        customer.getRole(),
                        customer.getPreferredCurrency(),
                        customer.getActive(),
                        customer.getCreatedAt(),
                        customer.getUpdatedAt());
            }

            return stringWriter.toString();
        } catch (IOException e) {
            throw new RuntimeException("Error generating CSV report", e);
        }
    }
}
