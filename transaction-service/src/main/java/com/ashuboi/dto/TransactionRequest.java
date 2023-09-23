package com.ashuboi.dto;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TransactionRequest {
    private Long fromUserId;

    private Long toUserId;

    private Double amount;

    private String remark;


}
