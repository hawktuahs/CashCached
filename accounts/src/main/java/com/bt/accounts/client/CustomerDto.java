package com.bt.accounts.client;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerDto {

    private Long id;
    private String customerId;
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private String address;
    private String city;
    private String state;
    private String zipCode;
    private String dateOfBirth;
    private String kycStatus;
    private String accountStatus;
    private String preferredCurrency;
    private String fullName;
}
