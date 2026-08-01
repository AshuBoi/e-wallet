package com.ashuboi.payload;

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
public class WalletUpdatedPayload {
    private String userName;
    private String userEmail;
    private BigDecimal balance;
    private String requestId;
}
