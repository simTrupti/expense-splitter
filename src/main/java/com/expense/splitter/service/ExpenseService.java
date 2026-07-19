package com.expense.splitter.service;

import com.expense.splitter.dto.ExpenseRequest;
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

        expense.setDescription(request.getDescription(request.getDescription()));
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


}
