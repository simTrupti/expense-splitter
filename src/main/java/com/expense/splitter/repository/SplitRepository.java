package com.expense.splitter.repository;

import com.expense.splitter.entity.Split;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SplitRepository extends JpaRepository<Split, Long> {

    List<Split> findByExpenseId(Long expenseId);
}
