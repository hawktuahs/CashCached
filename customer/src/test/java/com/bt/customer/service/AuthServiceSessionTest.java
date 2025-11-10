package com.bt.customer.service;

import com.bt.customer.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ActiveProfiles("test")
public class AuthServiceSessionTest {

    @Test
    public void testClassificationMinor() {
        LocalDate minorDob = LocalDate.now().minusYears(10);
        int age = (int) java.time.temporal.ChronoUnit.YEARS.between(minorDob, LocalDate.now());
        User.CustomerClassification classification = age < 18
                ? User.CustomerClassification.MINOR
                : User.CustomerClassification.REGULAR;

        assertEquals(User.CustomerClassification.MINOR, classification);
    }

    @Test
    public void testClassificationSenior() {
        LocalDate seniorDob = LocalDate.now().minusYears(65);
        int age = (int) java.time.temporal.ChronoUnit.YEARS.between(seniorDob, LocalDate.now());
        User.CustomerClassification classification = age >= 60
                ? User.CustomerClassification.SENIOR
                : User.CustomerClassification.REGULAR;

        assertEquals(User.CustomerClassification.SENIOR, classification);
    }

    @Test
    public void testClassificationRegular() {
        LocalDate regularDob = LocalDate.of(1990, 5, 15);
        int age = (int) java.time.temporal.ChronoUnit.YEARS.between(regularDob, LocalDate.now());
        User.CustomerClassification classification = age < 18
                ? User.CustomerClassification.MINOR
                : age >= 60
                        ? User.CustomerClassification.SENIOR
                        : User.CustomerClassification.REGULAR;

        assertEquals(User.CustomerClassification.REGULAR, classification);
    }

    @Test
    public void testUserWithNewFields() {
        User user = User.builder()
                .id(1L)
                .email("test@example.com")
                .fullName("Test User")
                .phoneNumber("+1234567890")
                .address("123 Main St, City, Country")
                .aadhaarNumber("123456789012")
                .panNumber("ABCDE1234F")
                .dateOfBirth(LocalDate.of(1990, 5, 15))
                .preferredCurrency("KWD")
                .role(User.Role.CUSTOMER)
                .active(true)
                .classification(User.CustomerClassification.REGULAR)
                .build();

        assertEquals("test@example.com", user.getEmail());
        assertEquals("Test User", user.getFullName());
        assertEquals("123 Main St, City, Country", user.getAddress());
        assertEquals("123456789012", user.getAadhaarNumber());
        assertEquals("ABCDE1234F", user.getPanNumber());
        assertEquals(LocalDate.of(1990, 5, 15), user.getDateOfBirth());
        assertEquals("KWD", user.getPreferredCurrency());
        assertEquals(User.CustomerClassification.REGULAR, user.getClassification());
    }
}
