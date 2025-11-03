package com.bt.fixeddeposit.dto.external;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerResponse {

    private Long id;
    private String username;
    private String email;
    private String fullName;
    private String phoneNumber;
    private String role;
    private Boolean active;
}
