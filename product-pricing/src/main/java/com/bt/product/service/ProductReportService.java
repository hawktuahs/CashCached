package com.bt.product.service;

import com.bt.product.entity.Currency;
import com.bt.product.entity.Product;
import com.bt.product.entity.ProductStatus;
import com.bt.product.entity.ProductType;
import com.bt.product.repository.ProductRepository;
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
public class ProductReportService {

    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public String generateProductReportCsv(
            ProductType productType,
            Currency currency,
            ProductStatus status,
            LocalDate effectiveDateFrom,
            LocalDate effectiveDateTo,
            Boolean requiresApproval) {

        List<Product> products = getFilteredProducts(
                productType, currency, status,
                effectiveDateFrom, effectiveDateTo, requiresApproval);

        return convertProductsToCsv(products);
    }

    private List<Product> getFilteredProducts(
            ProductType productType,
            Currency currency,
            ProductStatus status,
            LocalDate effectiveDateFrom,
            LocalDate effectiveDateTo,
            Boolean requiresApproval) {

        List<Product> products = productRepository.findAll();

        return products.stream()
                .filter(p -> productType == null || p.getProductType() == productType)
                .filter(p -> currency == null || p.getCurrency() == currency)
                .filter(p -> status == null || p.getStatus() == status)
                .filter(p -> effectiveDateFrom == null || p.getEffectiveDate().isAfter(effectiveDateFrom)
                        || p.getEffectiveDate().isEqual(effectiveDateFrom))
                .filter(p -> effectiveDateTo == null || p.getEffectiveDate().isBefore(effectiveDateTo)
                        || p.getEffectiveDate().isEqual(effectiveDateTo))
                .filter(p -> requiresApproval == null || p.getRequiresApproval().equals(requiresApproval))
                .collect(Collectors.toList());
    }

    private String convertProductsToCsv(List<Product> products) {
        StringWriter stringWriter = new StringWriter();
        String[] headers = { "Product Code", "Product Name", "Product Type", "Description",
                "Min Interest Rate", "Max Interest Rate", "Min Term (Months)", "Max Term (Months)",
                "Min Amount", "Max Amount", "Currency", "Status", "Effective Date", "Expiry Date",
                "Regulatory Code", "Requires Approval", "Compounding Frequency", "Created At", "Updated At" };

        try (CSVPrinter csvPrinter = new CSVPrinter(stringWriter, CSVFormat.DEFAULT)) {
            csvPrinter.printRecord((Object[]) headers);

            for (Product product : products) {
                csvPrinter.printRecord(
                        product.getProductCode(),
                        product.getProductName(),
                        product.getProductType(),
                        product.getDescription(),
                        product.getMinInterestRate(),
                        product.getMaxInterestRate(),
                        product.getMinTermMonths(),
                        product.getMaxTermMonths(),
                        product.getMinAmount(),
                        product.getMaxAmount(),
                        product.getCurrency(),
                        product.getStatus(),
                        product.getEffectiveDate(),
                        product.getExpiryDate(),
                        product.getRegulatoryCode(),
                        product.getRequiresApproval(),
                        product.getCompoundingFrequency(),
                        product.getCreatedAt(),
                        product.getUpdatedAt());
            }

            return stringWriter.toString();
        } catch (IOException e) {
            throw new RuntimeException("Error generating CSV report", e);
        }
    }
}
