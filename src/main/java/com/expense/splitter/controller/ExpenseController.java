package com.expense.splitter.controller;

import com.expense.splitter.dto.ExpenseRequest;
import com.expense.splitter.dto.TransactionResponse;
import com.expense.splitter.entity.Expense;
import com.expense.splitter.service.ExpenseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Tag(
        name = "Expenses",
        description = "Create expenses, calculate balances and simplify debts"
)
@RestController
@RequestMapping("/expenses")
public class ExpenseController {

    private final ExpenseService expenseService;

    public ExpenseController(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    @Operation(
            summary = "Create an expense",
            description = "Creates an expense using EQUAL, EXACT or PERCENTAGE splitting"
    )
    @PostMapping
    public Expense addExpense(@Valid @RequestBody ExpenseRequest request){
        return expenseService.addExpense(request);
    }

    @Operation(
            summary = "Calculate group net balances",
            description = "Calculates how much each user owes or should receive"
    )
    @GetMapping("/groups/{groupId}/net-balances")
    public Map<Long, BigDecimal> getNetBalances(@PathVariable Long groupId){
        return expenseService.calculateNetBalances(groupId);
    }

    @Operation(
            summary = "Simplify group debts",
            description = "Generates settlement transactions to reduce outstanding group debts"
    )
    @GetMapping("/groups/{groupId}/simplify")
    public List<TransactionResponse> simplifyBalances(
            @PathVariable Long groupId) {

        return expenseService.simplifyBalances(groupId);
    }
}
