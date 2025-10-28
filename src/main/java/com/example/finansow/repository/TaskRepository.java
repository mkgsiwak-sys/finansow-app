package com.example.finansow.repository;

import com.example.finansow.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    // Znajdź zadania wg terminu wykonania
    List<Task> findByAssigneeIdAndDateBetween(Long assigneeId, LocalDate startDate, LocalDate endDate);

    // DODANE: Dla Aktywnej Listy Zadań (wszystkie niezakończone)
    List<Task> findByAssigneeIdAndCompleted(Long assigneeId, boolean completed);

    // Dla Generatora Cyklicznych Zadań (sprawdza, czy już wygenerowano dla danej reguły)
    boolean existsByAssigneeIdAndRecurringOriginIdAndDate(
        Long assigneeId, Long recurringOriginId, LocalDate date);
    
    // DODANE: Dla Archiwizacji (znajdź ukończone zadania starsze niż X data)
    List<Task> findByAssigneeIdAndCompletedAndDateBefore(Long assigneeId, boolean completed, LocalDate date);
}