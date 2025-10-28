package com.example.finansow.repository;

import com.example.finansow.model.RecurrenceType;
import com.example.finansow.model.RecurringTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecurringTaskRepository extends JpaRepository<RecurringTask, Long> {

    // Znajdź wszystkie cykliczne zadania przypisane do danej osoby (wykonawcy)
    List<RecurringTask> findByAssigneeId(Long assigneeId);

    // Znajdź wszystkie zadania z danym typem powtórzenia (np. wszystkie DAILY)
    List<RecurringTask> findByRecurrenceType(RecurrenceType type);
    
    // Znajdź zadania miesięczne na dany dzień
    List<RecurringTask> findByRecurrenceTypeAndDayOfMonth(RecurrenceType type, int dayOfMonth);
    
    // Znajdź zadania cotygodniowe
    List<RecurringTask> findByRecurrenceTypeAndOnMonday(RecurrenceType type, boolean onMonday);
    List<RecurringTask> findByRecurrenceTypeAndOnTuesday(RecurrenceType type, boolean onTuesday);
    List<RecurringTask> findByRecurrenceTypeAndOnWednesday(RecurrenceType type, boolean onWednesday);
    List<RecurringTask> findByRecurrenceTypeAndOnThursday(RecurrenceType type, boolean onThursday);
    List<RecurringTask> findByRecurrenceTypeAndOnFriday(RecurrenceType type, boolean onFriday);
    List<RecurringTask> findByRecurrenceTypeAndOnSaturday(RecurrenceType type, boolean onSaturday);
    List<RecurringTask> findByRecurrenceTypeAndOnSunday(RecurrenceType type, boolean onSunday);
}
