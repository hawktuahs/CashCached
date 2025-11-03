package com.bt.product.client;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerDto {
    private Long id;
    private String username;
    private String email;
    private String fullName;
    private String phoneNumber;
    private String role;
    private Boolean active;
}
