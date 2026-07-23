package com.expense.splitter.service;

import com.expense.splitter.dto.ExpenseRequest;
import com.expense.splitter.dto.SplitRequest;
import com.expense.splitter.dto.TransactionResponse;
import com.expense.splitter.entity.*;
import com.expense.splitter.exception.ResourceNotFoundException;
import com.expense.splitter.repository.ExpenseRepository;
import com.expense.splitter.repository.GroupRepository;
import com.expense.splitter.repository.SplitRepository;
import com.expense.splitter.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional
    public Expense addExpense(ExpenseRequest request){

        //find the user who paid
        User paidBy = userRepository.findById(request.getPaidByUserId()).orElseThrow(() ->
                new ResourceNotFoundException(
                        "User not found with id: " + request.getPaidByUserId()
                ));

        //find the group
        Group group = groupRepository.findById(request.getGroupId()).orElseThrow(() ->
                new ResourceNotFoundException(
                        "Group not found with id: " + request.getGroupId()
                ));

        // 3. Create the expense
        Expense expense = new Expense();

        expense.setDescription(request.getDescription());
        expense.setAmount(request.getAmount());
        expense.setPaidBy(paidBy);
        expense.setGroup(group);
        expense.setCreatedAt(LocalDateTime.now());
        expense.setSplitType(request.getSplitType());

        // 4. Save expense
        Expense savedExpense = expenseRepository.save(expense);

        if (request.getSplitType() == SplitType.EQUAL) {

            if (request.getParticipantIds() == null
                    || request.getParticipantIds().isEmpty()) {

                throw new IllegalArgumentException(
                        "Participants are required for equal split"
                );
                }

            //calculate each person's equal share
            BigDecimal splitAmount = request.getAmount().divide(BigDecimal.valueOf(request.getParticipantIds().size()), 2, RoundingMode.HALF_UP);

            //create a aplit for every participants
            for (Long participantId : request.getParticipantIds()) {

                User participant = userRepository.findById(participantId).orElseThrow();

                Split split = new Split();
                split.setExpense(savedExpense);
                split.setUser(participant);
                split.setAmount(splitAmount);

                splitRepository.save(split);
            }
        } else if (request.getSplitType() == SplitType.EXACT) {

                if (request.getSplits() == null
                        || request.getSplits().isEmpty()) {

                    throw new IllegalArgumentException(
                            "Splits are required for exact split"
                    );
                }

            BigDecimal totalSplitAmount = BigDecimal.ZERO;

            for (SplitRequest splitRequest : request.getSplits()) {

                if (splitRequest.getAmount() == null
                        || splitRequest.getAmount().compareTo(BigDecimal.ZERO) <= 0) {

                    throw new IllegalArgumentException(
                            "Exact split amount must be greater than zero"
                    );
                }

                totalSplitAmount = totalSplitAmount.add(splitRequest.getAmount());
            }
            if (totalSplitAmount.compareTo(request.getAmount()) != 0) {
                throw new IllegalArgumentException(
                        "Split amounts must equal the total expense amount"
                );
            }
            for (SplitRequest splitRequest : request.getSplits()) {

                User participant = userRepository
                        .findById(splitRequest.getUserId())
                        .orElseThrow();

                Split split = new Split();

                split.setExpense(savedExpense);
                split.setUser(participant);
                split.setAmount(splitRequest.getAmount());

                splitRepository.save(split);
            }

        } else if (request.getSplitType() == SplitType.PERCENTAGE) {

            if (request.getSplits() == null
                    || request.getSplits().isEmpty()) {

                throw new IllegalArgumentException(
                        "Splits are required for percentage split"
                );
                }

        BigDecimal totalPercentage = BigDecimal.ZERO;

        for (SplitRequest splitRequest : request.getSplits()) {

            if (splitRequest.getPercentage() == null
                    || splitRequest.getPercentage().compareTo(BigDecimal.ZERO) <= 0) {

                throw new IllegalArgumentException(
                        "Percentage must be greater than zero"
                );
            }

            totalPercentage =
                    totalPercentage.add(splitRequest.getPercentage());
        }

        if (totalPercentage.compareTo(BigDecimal.valueOf(100)) != 0) {
            throw new IllegalArgumentException(
                    "Split percentages must add up to 100"
            );
        }

        for (SplitRequest splitRequest : request.getSplits()) {

            User participant = userRepository
                    .findById(splitRequest.getUserId())
                    .orElseThrow();

            BigDecimal splitAmount = request.getAmount()
                    .multiply(splitRequest.getPercentage())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

            Split split = new Split();

            split.setExpense(savedExpense);
            split.setUser(participant);
            split.setAmount(splitAmount);

            splitRepository.save(split);
        }

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
            // Update creditor balance
            balanceMap.put(
                    creditor,
                    balanceMap.get(creditor).subtract(settlementAmount)
            );

            // Update debtor balance
            balanceMap.put(
                    debtor,
                    balanceMap.get(debtor).add(settlementAmount)
            );

        }
        return transactions;

    }


}
