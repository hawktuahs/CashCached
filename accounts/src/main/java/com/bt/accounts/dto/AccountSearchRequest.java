package com.bt.accounts.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountSearchRequest {
    private String customerId;
    private String productCode;
    private String status;
    private String branchCode;
    @Builder.Default
    private Integer page = 0;
    @Builder.Default
    private Integer size = 10;
    @Builder.Default
    private String sortBy = "createdAt";
    @Builder.Default
    private String sortDirection = "DESC";
}
