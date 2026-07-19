package com.expense.splitter.service;

import com.expense.splitter.dto.ExpenseRequest;
import com.expense.splitter.dto.TransactionResponse;
import com.expense.splitter.entity.Expense;
import com.expense.splitter.entity.Group;
import com.expense.splitter.entity.Split;
import com.expense.splitter.entity.User;
import com.expense.splitter.repository.ExpenseRepository;
import com.expense.splitter.repository.GroupRepository;
import com.expense.splitter.repository.SplitRepository;
import com.expense.splitter.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Map;

import java.util.HashMap;
import java.util.List;

@Service
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final SplitRepository splitRepository;
    private final UserRepository userRepository;
    private final GroupRepository groupRepository;

    public ExpenseService(ExpenseRepository expenseRepository,
                          SplitRepository splitRepository,
                          UserRepository userRepository,
                          GroupRepository groupRepository) {

        this.expenseRepository = expenseRepository;
        this.splitRepository = splitRepository;
        this.userRepository = userRepository;
        this.groupRepository = groupRepository;
    }

    public Expense addExpense(ExpenseRequest request){

        //find the user who paid
        User paidBy = userRepository.findById(request.getPaidByUserId()).orElseThrow();

        //find the group
        Group group = groupRepository.findById(request.getGroupId()).orElseThrow();

        // 3. Create the expense
        Expense expense = new Expense();

        expense.setDescription(request.getDescription());
        expense.setAmount(request.getAmount());
        expense.setPaidBy(paidBy);
        expense.setGroup(group);
        expense.setCreatedAt(LocalDateTime.now());

        // 4. Save expense
        Expense savedExpense = expenseRepository.save(expense);

        //calculate each person's equal share
        BigDecimal splitAmount =  request.getAmount().divide(BigDecimal.valueOf(request.getParticipantIds().size()),2, RoundingMode.HALF_UP);

        //create a aplit for every participants
        for(Long participantId : request.getParticipantIds()){

            User participant = userRepository.findById(participantId).orElseThrow();

            Split split = new Split();
            split.setExpense(savedExpense);
            split.setUser(participant);
            split.setAmount(splitAmount);

            splitRepository.save(split);
        }

        return savedExpense;

    }

    public Map<Long, BigDecimal> calculateNetBalances(Long groupId) {

        List<Expense> expenses = expenseRepository.findByGroupId(groupId);

        Map<Long, BigDecimal> balanceMap = new HashMap<>();

        for (Expense expense : expenses) {

            List<Split> splits = splitRepository.findByExpenseId(expense.getId());

            Long payerId = expense.getPaidBy().getId();

            balanceMap.put(payerId,balanceMap.getOrDefault(payerId, BigDecimal.ZERO).add(expense.getAmount()));

            for (Split split : splits) {

                Long participantId = split.getUser().getId();

                balanceMap.put(
                        participantId,
                        balanceMap.getOrDefault(participantId, BigDecimal.ZERO)
                                .subtract(split.getAmount())
                );
            }

        }
        return balanceMap;

    }

    public List<TransactionResponse> simplifyBalances(Long groupId) {

        Map<Long, BigDecimal> balanceMap = calculateNetBalances(groupId);

        List<TransactionResponse> transactions = new ArrayList<>();

        while (true) {

            Long creditor = null;
            Long debtor = null;

            BigDecimal maxCredit = BigDecimal.ZERO;
            BigDecimal maxDebit = BigDecimal.ZERO;

            for (Map.Entry<Long, BigDecimal> entry : balanceMap.entrySet()) {

                BigDecimal balance = entry.getValue();

                if (balance.compareTo(maxCredit) > 0) {
                    maxCredit = balance;
                    creditor = entry.getKey();
                }

                if (balance.compareTo(maxDebit) < 0) {
                    maxDebit = balance;
                    debtor = entry.getKey();
                }
            }

            if (creditor == null || debtor == null
                    || maxCredit.compareTo(BigDecimal.ZERO) == 0
                    || maxDebit.compareTo(BigDecimal.ZERO) == 0) {

                break;
            }

            BigDecimal settlementAmount =
                    maxCredit.min(maxDebit.abs());

            User creditorUser = userRepository.findById(creditor)
                    .orElseThrow();

            User debtorUser = userRepository.findById(debtor)
                    .orElseThrow();

            transactions.add(
                    new TransactionResponse(
                            debtorUser.getName(),
                            creditorUser.getName(),
                            settlementAmount
                    )
            );

        }
        return transactions;

    }


}
