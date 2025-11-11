package com.example.finansow.model;

import jakarta.persistence.*;
// import lombok.Data; // Usunęliśmy Lombok
// import lombok.NoArgsConstructor; // Usunęliśmy Lombok

import java.util.List;

@Entity
// @Data // Usunięte!
// @NoArgsConstructor // Usunięte!
public class Person {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String color;

    @OneToMany(mappedBy = "person", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Transaction> transactions;

    @OneToMany(mappedBy = "assignee", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Task> assignedTasks;

    // --- Konstruktory (pozostają) ---
    public Person() {
        // Pusty konstruktor wymagany przez JPA
    }

    public Person(String name, String color) {
        this.name = name;
        this.color = color;
    }

    // --- RĘCZNIE DODANE GETTERY I SETTERY (Zamiast @Data) ---

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public List<Transaction> getTransactions() {
        return transactions;
    }

    public void setTransactions(List<Transaction> transactions) {
        this.transactions = transactions;
    }

    public List<Task> getAssignedTasks() {
        return assignedTasks;
    }

    public void setAssignedTasks(List<Task> assignedTasks) {
        this.assignedTasks = assignedTasks;
    }
}

