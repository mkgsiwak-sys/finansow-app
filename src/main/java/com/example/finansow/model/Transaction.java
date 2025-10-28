package com.example.finansow.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private String description;
    private BigDecimal amount;
    private LocalDate date; // Data księgowania/utworzenia

    @Enumerated(EnumType.STRING)
    private TransactionType type; // INCOME lub EXPENSE

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "person_id")
    private Person person;

    // Pola Faktury
    private boolean paid;
    private boolean invoiced;
    private String invoiceNumber;

    // NOWE POLE: Termin płatności dla niezapłaconych wydatków
    private LocalDate dueDate;

    // Pole dla reguł cyklicznych (musi być użyte w generatorze)
    private Long recurringOriginId;


    // --- Konstruktory ---
    public Transaction() {
        // Pusty konstruktor wymagany przez JPA
    }
    
    // Pełny konstruktor dla transakcji ręcznych
    public Transaction(String description, BigDecimal amount, LocalDate date, TransactionType type, Person person, boolean paid, boolean invoiced, String invoiceNumber, LocalDate dueDate) {
        this.description = description;
        this.amount = amount;
        this.date = date;
        this.type = type;
        this.person = person;
        this.paid = paid;
        this.invoiced = invoiced;
        this.invoiceNumber = invoiceNumber;
        this.dueDate = dueDate; 
    }

    // Konstruktor dla transakcji automatycznych (z serwisu)
    public Transaction(String description, BigDecimal amount, LocalDate date, TransactionType type, Person person) {
        this.description = description;
        this.amount = amount;
        this.date = date;
        this.type = type;
        this.person = person;
        this.paid = false;
        this.invoiced = false;
    }

    // --- RĘCZNIE DODANE GETTERY I SETTERY ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public TransactionType getType() { return type; }
    public void setType(TransactionType type) { this.type = type; }
    public Person getPerson() { return person; }
    public void setPerson(Person person) { this.person = person; }
    public boolean isPaid() { return paid; }
    public void setPaid(boolean paid) { this.paid = paid; }
    public boolean isInvoiced() { return invoiced; }
    public void setInvoiced(boolean invoiced) { this.invoiced = invoiced; }
    public String getInvoiceNumber() { return invoiceNumber; }
    public void setInvoiceNumber(String invoiceNumber) { this.invoiceNumber = invoiceNumber; }
    public Long getRecurringOriginId() { return recurringOriginId; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }

    // POPRAWKA: Setter dla OriginId, wymagany przez generator
    public void setRecurringOriginId(Long recurringOriginId) { 
        this.recurringOriginId = recurringOriginId;
    }
}