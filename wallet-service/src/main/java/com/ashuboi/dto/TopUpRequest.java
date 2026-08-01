package com.ashuboi.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class TopUpRequest {
    @NotNull
    @DecimalMin(value = "1.00", message = "Minimum top-up is 1.00")
    private BigDecimal amount;
}
