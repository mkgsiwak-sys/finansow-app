package com.example.finansow.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigner_id")
    private Person assigner; // Kto zlecił

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id")
    private Person assignee; // Na kogo zlecono

    private LocalDate date;
    private boolean completed;

    // Pole dla reguł cyklicznych (musi być użyte w generatorze)
    private Long recurringOriginId;


    // --- Konstruktory (pozostają) ---
    public Task() {
        // Pusty konstruktor wymagany przez JPA
    }

    public Task(String description, Person assigner, Person assignee, LocalDate date, boolean completed) {
        this.description = description;
        this.assigner = assigner;
        this.assignee = assignee;
        this.date = date;
        this.completed = completed;
    }

    // --- RĘCZNIE DODANE GETTERY I SETTERY ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Person getAssigner() { return assigner; }
    public void setAssigner(Person assigner) { this.assigner = assigner; }
    public Person getAssignee() { return assignee; }
    public void setAssignee(Person assignee) { this.assignee = assignee; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }
    public Long getRecurringOriginId() { return recurringOriginId; }
    
    // POPRAWKA: Setter dla OriginId, wymagany przez generator
    public void setRecurringOriginId(Long recurringOriginId) { 
        this.recurringOriginId = recurringOriginId;
    }
}