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

    // <<< ZMIANA: Lista zadań tylko na DZIŚ >>>
    List<Task> findByAssigneeIdAndCompletedAndDate(Long assigneeId, boolean completed, LocalDate date);

    // Dla Generatora Cyklicznych Zadań (sprawdza, czy już wygenerowano dla danej reguły)
    boolean existsByAssigneeIdAndRecurringOriginIdAndDate(
            Long assigneeId, Long recurringOriginId, LocalDate date);

    // <<< POPRAWKA: Usunięto duplikat. Ta metoda jest używana dla zadań zaległych ORAZ archiwizacji >>>
    List<Task> findByAssigneeIdAndCompletedAndDateBefore(Long assigneeId, boolean completed, LocalDate date);

    // <<< NOWE METODY DO USUWANIA OSOBY >>>
    void deleteAllByAssigneeId(Long assigneeId);
    void deleteAllByAssignerId(Long assignerId);
}