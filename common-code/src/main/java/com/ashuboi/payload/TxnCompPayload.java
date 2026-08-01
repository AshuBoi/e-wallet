package com.ashuboi.payload;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TxnCompPayload {
    private Long id;
    private Boolean success;
    private String reason;
    private String requestId;
}
