package com.expense.splitter.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class ExpenseRequest {

    private String description;

    private BigDecimal amount;

    private Long paidByUserId;

    private Long groupId;

    private List<Long> participantIds;
}
