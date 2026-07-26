package com.expense.splitter.dto;

import com.expense.splitter.entity.SplitType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;


import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class ExpenseRequest {

    @NotBlank(message = "Description is required")
    private String description;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be greater than zero")
    private BigDecimal amount;

    @NotNull(message = "Group id is required")
    private Long groupId;

    @NotNull(message = "Split type is required")
    private SplitType splitType;


    // Used for EQUAL splitting
    private List<Long> participantIds;

    // Used for EXACT splitting
    private List<SplitRequest> splits;

}
