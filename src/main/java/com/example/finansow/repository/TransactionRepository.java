package com.example.finansow.repository;

import com.example.finansow.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    // Znajdź transakcje wg daty księgowania (do bilansu i raportów)
    List<Transaction> findByPersonIdAndDateBetween(Long personId, LocalDate startDate, LocalDate endDate);

    // Znajdź niezapłacone transakcje wg terminu płatności (do kalendarza i nowej karty)
    List<Transaction> findByPersonIdAndPaidAndDueDateBetween(Long personId, boolean paid, LocalDate startDate, LocalDate endDate);

    // DODANE: Dla Bilansu Skumulowanego (zaksięgowane transakcje do danej daty)
    List<Transaction> findByPersonIdAndDateBefore(Long personId, LocalDate date);

    // POPRAWKA: Dla Generatora Stałych Transakcji (Użycie RecurringOriginId)
    boolean existsByPersonIdAndRecurringOriginIdAndDateBetween(
        Long personId, Long recurringOriginId, LocalDate startDate, LocalDate endDate);
}