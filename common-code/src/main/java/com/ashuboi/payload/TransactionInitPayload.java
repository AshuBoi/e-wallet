package com.ashuboi.payload;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class TransactionInitPayload {
    private Long id;
    private String txnId;
    private Long fromUserId;
    private Long toUserId;
    private BigDecimal amount;
    private String remark;
    private String requestId;
}
