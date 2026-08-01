package com.ashuboi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TopUpResponse {
    private String sessionId;
    private String provider;
    private String checkoutUrl;
    private BigDecimal amount;
    private String status;
}
