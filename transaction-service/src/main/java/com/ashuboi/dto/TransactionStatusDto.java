package com.ashuboi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TransactionStatusDto {
    private String txnId;
    private String status;
    private String reason;
    private Long fromUserId;
    private Long toUserId;
    private BigDecimal amount;
    private String remark;
}
