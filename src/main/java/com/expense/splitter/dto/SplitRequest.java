package com.expense.splitter.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class SplitRequest {

    private Long userId;

    private BigDecimal amount;

    private BigDecimal percentage;
}
