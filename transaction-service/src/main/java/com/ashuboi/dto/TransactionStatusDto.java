package com.ashuboi.dto;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class TransactionStatusDto {

    private String status;
    private String reason;
}