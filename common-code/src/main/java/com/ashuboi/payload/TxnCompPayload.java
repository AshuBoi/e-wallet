package com.ashuboi.payload;
import lombok.*;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TxnCompPayload {
    private Long id;
    private Boolean success;
    private String reason;
    private String requestId;
}