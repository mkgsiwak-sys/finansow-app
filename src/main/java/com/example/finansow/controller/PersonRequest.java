package com.example.finansow.controller;

// Ta klasa będzie modelem dla danych wysyłanych z formularza
public class PersonRequest {

    // Nazwa pola "name" musi być zgodna z tym, co wysyła Twój frontend (JavaScript)
    // Jeśli w JS masz np. { "imie": "Marek" }, zmień "name" na "imie"
    private String name;

    // Gettery i Settery są niezbędne dla Springa do mapowania JSONa
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}