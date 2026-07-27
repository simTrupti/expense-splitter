package com.expense.splitter;

import com.expense.splitter.dto.ExpenseRequest;
import com.expense.splitter.dto.SplitRequest;
import com.expense.splitter.dto.TransactionResponse;
import com.expense.splitter.entity.*;
import com.expense.splitter.repository.*;
import com.expense.splitter.service.ExpenseService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
public class ExpenseServiceTest {

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private SplitRepository splitRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private GroupRepository groupRepository;

    @Mock
    private GroupMemberRepository groupMemberRepository;

    @InjectMocks
    private ExpenseService expenseService;

    @Test
    void shouldCalculateNetBalances() {

        Long groupId = 1L;

        User trupti = new User();
        trupti.setId(1L);

        User rahul = new User();
        rahul.setId(2L);

        User alisha = new User();
        alisha.setId(3L);

        Expense expense = new Expense();
        expense.setId(1L);
        expense.setAmount(new BigDecimal("600"));
        expense.setPaidBy(trupti);

        Split split1 = new Split();
        split1.setUser(trupti);
        split1.setAmount(new BigDecimal("200"));

        Split split2 = new Split();
        split2.setUser(rahul);
        split2.setAmount(new BigDecimal("200"));

        Split split3 = new Split();
        split3.setUser(alisha);
        split3.setAmount(new BigDecimal("200"));

        when(expenseRepository.findByGroupId(groupId))
                .thenReturn(List.of(expense));

        when(splitRepository.findByExpenseId(1L))
                .thenReturn(List.of(split1, split2, split3));

        Map<Long, BigDecimal> result =
                expenseService.calculateNetBalances(groupId);

        assertEquals(new BigDecimal("400"), result.get(1L));
        assertEquals(new BigDecimal("-200"), result.get(2L));
        assertEquals(new BigDecimal("-200"), result.get(3L));
    }

    @Test
    void shouldSimplifyBalances() {

        Long groupId = 1L;

        User trupti = new User();
        trupti.setId(1L);
        trupti.setName("Trupti");

        User rahul = new User();
        rahul.setId(2L);
        rahul.setName("Rahul");

        User alisha = new User();
        alisha.setId(3L);
        alisha.setName("Alisha");

        Expense expense = new Expense();
        expense.setId(1L);
        expense.setAmount(new BigDecimal("600"));
        expense.setPaidBy(trupti);

        Split split1 = new Split();
        split1.setUser(trupti);
        split1.setAmount(new BigDecimal("200"));

        Split split2 = new Split();
        split2.setUser(rahul);
        split2.setAmount(new BigDecimal("200"));

        Split split3 = new Split();
        split3.setUser(alisha);
        split3.setAmount(new BigDecimal("200"));

        when(expenseRepository.findByGroupId(groupId))
                .thenReturn(List.of(expense));

        when(splitRepository.findByExpenseId(1L))
                .thenReturn(List.of(split1, split2, split3));

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(trupti));

        when(userRepository.findById(2L))
                .thenReturn(Optional.of(rahul));

        when(userRepository.findById(3L))
                .thenReturn(Optional.of(alisha));

        List<TransactionResponse> result =
                expenseService.simplifyBalances(groupId);

        assertEquals(2, result.size());

        for (TransactionResponse transaction : result) {

            assertEquals("Trupti", transaction.getToUser());
            assertEquals(new BigDecimal("200"), transaction.getAmount());
        }
    }

    @Test
    void shouldCreateEqualSplits() {

        // ARRANGE

        User trupti = new User();
        trupti.setId(1L);
        trupti.setName("Trupti");
        trupti.setEmail("trupti@gmail.com");

        User rahul = new User();
        rahul.setId(2L);

        User alisha = new User();
        alisha.setId(3L);


        Group group = new Group();
        group.setId(1L);
        group.setName("Goa Trip");


        ExpenseRequest request = new ExpenseRequest();

        request.setDescription("Dinner");
        request.setAmount(new BigDecimal("600"));
        request.setGroupId(1L);
        request.setSplitType(SplitType.EQUAL);
        request.setParticipantIds(
                List.of(1L, 2L, 3L)
        );


        // Fake logged-in user

        Authentication authentication =
                mock(Authentication.class);

        when(authentication.getName())
                .thenReturn("trupti@gmail.com");

        SecurityContextHolder
                .getContext()
                .setAuthentication(authentication);


        // MOCK

        when(userRepository.findByEmail("trupti@gmail.com"))
                .thenReturn(Optional.of(trupti));

        when(groupRepository.findById(1L))
                .thenReturn(Optional.of(group));

        when(groupMemberRepository
                .existsByGroupIdAndUserId(1L, 1L))
                .thenReturn(true);

        when(expenseRepository.save(any(Expense.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0)
                );

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(trupti));

        when(userRepository.findById(2L))
                .thenReturn(Optional.of(rahul));

        when(userRepository.findById(3L))
                .thenReturn(Optional.of(alisha));


        // ACT

        expenseService.addExpense(request);


        // ASSERT

        verify(splitRepository, times(3))
                .save(any(Split.class));
    }


    // =========================================================
    // TEST 4 - INVALID EXACT SPLIT
    // =========================================================

    @Test
    void shouldRejectExactSplitsWhenTotalDoesNotMatch() {

        // ARRANGE

        User trupti = new User();
        trupti.setId(1L);
        trupti.setEmail("trupti@gmail.com");


        Group group = new Group();
        group.setId(1L);


        SplitRequest split1 = new SplitRequest();
        split1.setUserId(1L);
        split1.setAmount(new BigDecimal("300"));

        SplitRequest split2 = new SplitRequest();
        split2.setUserId(2L);
        split2.setAmount(new BigDecimal("200"));


        ExpenseRequest request = new ExpenseRequest();

        request.setDescription("Dinner");
        request.setAmount(new BigDecimal("600"));
        request.setGroupId(1L);
        request.setSplitType(SplitType.EXACT);
        request.setSplits(
                List.of(split1, split2)
        );


        // Fake logged-in user

        Authentication authentication =
                mock(Authentication.class);

        when(authentication.getName())
                .thenReturn("trupti@gmail.com");

        SecurityContextHolder
                .getContext()
                .setAuthentication(authentication);


        // MOCK

        when(userRepository.findByEmail("trupti@gmail.com"))
                .thenReturn(Optional.of(trupti));

        when(groupRepository.findById(1L))
                .thenReturn(Optional.of(group));

        when(groupMemberRepository
                .existsByGroupIdAndUserId(1L, 1L))
                .thenReturn(true);

        when(expenseRepository.save(any(Expense.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0)
                );


        // ACT + ASSERT

        assertThrows(
                IllegalArgumentException.class,
                () -> expenseService.addExpense(request)
        );
    }


    // =========================================================
    // TEST 5 - INVALID PERCENTAGE
    // =========================================================

    @Test
    void shouldRejectPercentageWhenNotHundred() {

        // ARRANGE

        User trupti = new User();
        trupti.setId(1L);
        trupti.setEmail("trupti@gmail.com");


        Group group = new Group();
        group.setId(1L);


        SplitRequest split1 = new SplitRequest();
        split1.setUserId(1L);
        split1.setPercentage(
                new BigDecimal("40")
        );

        SplitRequest split2 = new SplitRequest();
        split2.setUserId(2L);
        split2.setPercentage(
                new BigDecimal("30")
        );


        ExpenseRequest request = new ExpenseRequest();

        request.setDescription("Hotel");
        request.setAmount(new BigDecimal("1000"));
        request.setGroupId(1L);
        request.setSplitType(SplitType.PERCENTAGE);
        request.setSplits(
                List.of(split1, split2)
        );


        // Fake logged-in user

        Authentication authentication =
                mock(Authentication.class);

        when(authentication.getName())
                .thenReturn("trupti@gmail.com");

        SecurityContextHolder
                .getContext()
                .setAuthentication(authentication);


        // MOCK

        when(userRepository.findByEmail("trupti@gmail.com"))
                .thenReturn(Optional.of(trupti));

        when(groupRepository.findById(1L))
                .thenReturn(Optional.of(group));

        when(groupMemberRepository
                .existsByGroupIdAndUserId(1L, 1L))
                .thenReturn(true);

        when(expenseRepository.save(any(Expense.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0)
                );


        // ACT + ASSERT

        assertThrows(
                IllegalArgumentException.class,
                () -> expenseService.addExpense(request)
        );
    }


    // =========================================================
    // TEST 6 - NON MEMBER CANNOT ADD EXPENSE
    // =========================================================

    @Test
    void shouldRejectExpenseWhenUserIsNotGroupMember() {

        // ARRANGE

        User trupti = new User();
        trupti.setId(1L);
        trupti.setEmail("trupti@gmail.com");


        Group group = new Group();
        group.setId(1L);
        group.setName("Goa Trip");


        ExpenseRequest request = new ExpenseRequest();

        request.setDescription("Dinner");
        request.setAmount(new BigDecimal("600"));
        request.setGroupId(1L);
        request.setSplitType(SplitType.EQUAL);
        request.setParticipantIds(
                List.of(1L, 2L, 3L)
        );


        // Fake logged-in user

        Authentication authentication =
                mock(Authentication.class);

        when(authentication.getName())
                .thenReturn("trupti@gmail.com");

        SecurityContextHolder
                .getContext()
                .setAuthentication(authentication);


        // MOCK

        when(userRepository.findByEmail("trupti@gmail.com"))
                .thenReturn(Optional.of(trupti));

        when(groupRepository.findById(1L))
                .thenReturn(Optional.of(group));

        // IMPORTANT:
        // Trupti is NOT a member
        when(groupMemberRepository
                .existsByGroupIdAndUserId(1L, 1L))
                .thenReturn(false);


        // ACT + ASSERT

        assertThrows(
                AccessDeniedException.class,
                () -> expenseService.addExpense(request)
        );
    }
}



