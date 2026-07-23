package com.expense.splitter.controller;

import com.expense.splitter.dto.ExpenseRequest;
import com.expense.splitter.dto.TransactionResponse;
import com.expense.splitter.entity.Expense;
import com.expense.splitter.service.ExpenseService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/expenses")
public class ExpenseController {

    private final ExpenseService expenseService;

    public ExpenseController(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    @PostMapping
    public Expense addExpense(@Valid @RequestBody ExpenseRequest request){
        return expenseService.addExpense(request);
    }

    @GetMapping("/groups/{groupId}/net-balances")
    public Map<Long, BigDecimal> getNetBalances(@PathVariable Long groupId){
        return expenseService.calculateNetBalances(groupId);
    }

    @GetMapping("/groups/{groupId}/simplify")
    public List<TransactionResponse> simplifyBalances(
            @PathVariable Long groupId) {

        return expenseService.simplifyBalances(groupId);
    }
}
