package com.example.finansow.model;

/**
 * Klasa pomocnicza przechowująca informacje o pojedynczym wydarzeniu 
 * w widoku szczegółów dnia (Modal).
 */
public class DayEvent {
    
    private String description;
    private String type; // ZADANIE, WYDATEK, STAŁA PŁATNOŚĆ
    private String details; // Dodatkowe info (kwota, termin)
    private String colorClass; // np. text-red-500
    private String actionUrl; // URL do zmiany statusu/usunięcia (opcjonalnie)
    
    // Używane do sortowania
    private int priority; // 1=Niezapłacone/Ważne, 2=Zadanie, 3=Zaplanowane

    public DayEvent(String description, String type, String details, String colorClass, String actionUrl, int priority) {
        this.description = description;
        this.type = type;
        this.details = details;
        this.colorClass = colorClass;
        this.actionUrl = actionUrl;
        this.priority = priority;
    }

    // --- RĘCZNIE DODANE GETTERY ---

    public String getDescription() {
        return description;
    }

    public String getType() {
        return type;
    }

    public String getDetails() {
        return details;
    }

    public String getColorClass() {
        return colorClass;
    }

    public String getActionUrl() {
        return actionUrl;
    }

    public int getPriority() {
        return priority;
    }
}