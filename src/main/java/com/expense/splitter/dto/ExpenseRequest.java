package com.expense.splitter.dto;

import com.expense.splitter.entity.SplitType;
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


    // Used for EQUAL splitting
    private List<Long> participantIds;

    // Used for EXACT splitting
    private List<SplitRequest> splits;

    private SplitType splitType;
}
