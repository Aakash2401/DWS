package com.dws.challenge.domain;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Data
public class TransferRequest {

    @NotNull
    private String accountFrom;

    @NotNull
    private String accountTo;

    @NotNull
    @Min(value = 1, message = "Transfer amount must be positive")
    private BigDecimal amount;
}
