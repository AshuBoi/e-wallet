package com.ashuboi.payload;

import lombok.*;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class TransactionInitPayload {
    private Long id;
    private Long fromUserId;

    private Long toUserId;

    private Double amount;

    private String remark;
    private String requestId;
}
