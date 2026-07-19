package com.expense.splitter.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class TransactionResponse {

    private String fromUser;

    private String toUser;

    private BigDecimal amount;
}
