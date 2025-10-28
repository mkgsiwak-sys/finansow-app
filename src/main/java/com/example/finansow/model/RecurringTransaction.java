package com.example.finansow.model;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
public class RecurringTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "person_id")
    private Person person;

    private String description;
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    private TransactionType type; // INCOME lub EXPENSE

    // Dzień miesiąca, w którym ma się wygenerować (np. 10)
    private int dayOfMonthToGenerate; 

    // NOWE POLE: Ile dni po wygenerowaniu ustawić termin płatności
    // np. 0 = termin tego samego dnia, 7 = termin za tydzień
    private int daysUntilDue;

    // --- Konstruktory ---
    public RecurringTransaction() {
        // Pusty konstruktor wymagany przez JPA
    }

    public RecurringTransaction(Person person, String description, BigDecimal amount, TransactionType type, int dayOfMonthToGenerate) {
        this.person = person;
        this.description = description;
        this.amount = amount;
        this.type = type;
        this.dayOfMonthToGenerate = dayOfMonthToGenerate;
        this.daysUntilDue = 0; // Domyślnie
    }

    // --- RĘCZNIE DODANE GETTERY I SETTERY ---

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Person getPerson() {
        return person;
    }

    public void setPerson(Person person) {
        this.person = person;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public TransactionType getType() {
        return type;
    }

    public void setType(TransactionType type) {
        this.type = type;
    }

    public int getDayOfMonthToGenerate() {
        return dayOfMonthToGenerate;
    }

    public void setDayOfMonthToGenerate(int dayOfMonthToGenerate) {
        this.dayOfMonthToGenerate = dayOfMonthToGenerate;
    }

    // NOWE GETTERY/SETTERY
    public int getDaysUntilDue() {
        return daysUntilDue;
    }

    public void setDaysUntilDue(int daysUntilDue) {
        this.daysUntilDue = daysUntilDue;
    }
}

