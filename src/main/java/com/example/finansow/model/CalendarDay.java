package com.example.finansow.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Klasa pomocnicza (niezapisywana w bazie), która reprezentuje jeden dzień
 * w naszym widoku kalendarza. Przechowuje wszystkie wydarzenia z danego dnia.
 */
public class CalendarDay {

    private LocalDate date;
    private boolean isCurrentMonth; // Czy dzień należy do bieżącego miesiąca (czy jest "szary")
    private boolean isToday;

    // Listy wydarzeń na ten dzień
    private List<Task> tasks = new ArrayList<>(); // Pojedyncze zadania
    private List<Transaction> unpaidExpenses = new ArrayList<>(); // Niezapłacone wydatki
    private List<RecurringTransaction> recurringPayments = new ArrayList<>(); // Info o stałych płatnościach
    
    // NOWA LISTA: Zadania cykliczne zaplanowane na ten dzień
    private List<RecurringTask> recurringTasks = new ArrayList<>(); 

    public CalendarDay(LocalDate date, boolean isCurrentMonth) {
        this.date = date;
        this.isCurrentMonth = isCurrentMonth;
        this.isToday = date.equals(LocalDate.now());
    }

    // Metody do dodawania wydarzeń
    public void addTask(Task task) {
        this.tasks.add(task);
    }

    public void addUnpaidExpense(Transaction transaction) {
        this.unpaidExpenses.add(transaction);
    }
    
    public void addRecurringPayment(RecurringTransaction recurringTransaction) {
        this.recurringPayments.add(recurringTransaction);
    }
    
    // NOWA METODA
    public void addRecurringTask(RecurringTask recurringTask) {
        this.recurringTasks.add(recurringTask);
    }

    // --- Standardowe Gettery ---
    
    public LocalDate getDate() {
        return date;
    }

    public boolean isCurrentMonth() {
        return isCurrentMonth;
    }

    public boolean isToday() {
        return isToday;
    }

    public List<Task> getTasks() {
        return tasks;
    }

    public List<Transaction> getUnpaidExpenses() {
        return unpaidExpenses;
    }

    public List<RecurringTransaction> getRecurringPayments() {
        return recurringPayments;
    }
    
    // NOWY GETTER
    public List<RecurringTask> getRecurringTasks() {
        return recurringTasks;
    }
    
    public int getDayOfMonth() {
        return date.getDayOfMonth();
    }
}

