package com.example.finansow.repository;

import com.example.finansow.model.RecurringTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecurringTransactionRepository extends JpaRepository<RecurringTransaction, Long> {

    // Znajdź wszystkie stałe transakcje dla danej osoby
    List<RecurringTransaction> findByPersonId(Long personId);

    // POPRAWIONA NAZWA METODY: Znajdź wszystkie stałe transakcje, które mają się wygenerować danego dnia miesiąca
    List<RecurringTransaction> findByDayOfMonthToGenerate(int dayOfMonthToGenerate);

    // <<< NOWA METODA DO USUWANIA OSOBY >>>
    void deleteAllByPersonId(Long personId);
}