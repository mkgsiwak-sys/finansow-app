package com.example.finansow.service;

import com.example.finansow.model.*;
import com.example.finansow.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.math.BigDecimal;
import java.util.List;

@Service
public class RecurringService {

    @Autowired private TransactionRepository transactionRepository;
    @Autowired private RecurringTransactionRepository recurringTransactionRepo;
    @Autowired private RecurringTaskRepository recurringTaskRepo;
    @Autowired private PersonRepository personRepository;
    @Autowired private TaskRepository taskRepository;

    /**
     * Generuje Transakcje i Zadania na dany miesiąc bazując na regułach cyklicznych.
     */
    public void generateRecurringItems(Long personId, YearMonth targetMonth) {
        LocalDate startDate = targetMonth.atDay(1);
        LocalDate endDate = targetMonth.atEndOfMonth();

        Person person = personRepository.findById(personId)
                .orElseThrow(() -> new IllegalArgumentException("Osoba o ID " + personId + " nie została znaleziona."));

        // A. GENEROWANIE STAŁYCH TRANSAKCJI
        List<RecurringTransaction> recurringTransactions = recurringTransactionRepo.findByPersonId(personId);
        
        for (RecurringTransaction rt : recurringTransactions) {
            LocalDate transactionDate;
            try {
                // Generuj datę na podstawie dnia miesiąca
                transactionDate = targetMonth.atDay(rt.getDayOfMonthToGenerate());
            } catch (Exception e) {
                // Jeśli dzień miesiąca jest nieprawidłowy (np. 31 w lutym), użyj końca miesiąca
                transactionDate = targetMonth.atEndOfMonth();
            }
            
            // Sprawdzanie, czy transakcja została już wygenerowana dla tego miesiąca
            boolean exists = transactionRepository.existsByPersonIdAndRecurringOriginIdAndDateBetween(
                personId, rt.getId(), startDate, endDate
            );

            if (!exists) {
                LocalDate dueDate = transactionDate.plusDays(rt.getDaysUntilDue());

                Transaction newTransaction = new Transaction(
                    rt.getDescription(),
                    rt.getAmount(),
                    transactionDate, 
                    rt.getType(),
                    person,
                    false, // paid = false
                    false,
                    null,  
                    dueDate
                );
                newTransaction.setRecurringOriginId(rt.getId());
                transactionRepository.save(newTransaction);
            }
        }
        
        // B. GENEROWANIE CYKLICZNYCH ZADAŃ
        List<RecurringTask> recurringTasks = recurringTaskRepo.findByAssigneeId(personId);
        
        for (RecurringTask rtask : recurringTasks) {
            LocalDate currentDate = startDate;

            while (!currentDate.isAfter(endDate)) {
                boolean shouldGenerate = false;
                
                // 1. Sprawdzanie warunku generacji
                if (rtask.getRecurrenceType() == RecurrenceType.MONTHLY && rtask.getDayOfMonth() == currentDate.getDayOfMonth()) {
                    shouldGenerate = true;
                } else if (rtask.getRecurrenceType() == RecurrenceType.WEEKLY) {
                    DayOfWeek dayOfWeek = currentDate.getDayOfWeek();
                    switch (dayOfWeek) {
                        case MONDAY: shouldGenerate = rtask.isOnMonday(); break;
                        case TUESDAY: shouldGenerate = rtask.isOnTuesday(); break;
                        case WEDNESDAY: shouldGenerate = rtask.isOnWednesday(); break;
                        case THURSDAY: shouldGenerate = rtask.isOnThursday(); break;
                        case FRIDAY: shouldGenerate = rtask.isOnFriday(); break;
                        case SATURDAY: shouldGenerate = rtask.isOnSaturday(); break;
                        case SUNDAY: shouldGenerate = rtask.isOnSunday(); break;
                    }
                } else if (rtask.getRecurrenceType() == RecurrenceType.DAILY) {
                    shouldGenerate = true;
                }

                // 2. Jeśli należy wygenerować i zadanie nie istnieje
                if (shouldGenerate) {
                    boolean exists = taskRepository.existsByAssigneeIdAndRecurringOriginIdAndDate(
                        personId, rtask.getId(), currentDate
                    );

                    if (!exists) {
                         Task newTask = new Task(
                            rtask.getDescription(),
                            rtask.getAssigner(),
                            rtask.getAssignee(),
                            currentDate, 
                            false
                        );
                        newTask.setRecurringOriginId(rtask.getId());
                        taskRepository.save(newTask);
                    }
                }
                currentDate = currentDate.plusDays(1);
            }
        }
    }
}