package com.bt.fixeddeposit.dto.external;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExternalApiResponse<T> {

    private Boolean success;
    private String message;
    private T data;
    private Long timestamp;
}
