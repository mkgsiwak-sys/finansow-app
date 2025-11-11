package com.example.finansow.controller;

import com.example.finansow.service.PersonService;
// Importujemy klasę PersonRequest (jeśli jej nie masz, stwórz ją tak jak w poprzednich krokach)
import com.example.finansow.controller.PersonRequest;

// WAŻNA ZMIANA: Zmieniamy @RestController na @Controller
// @RestController = każda metoda zwraca JSON
// @Controller = metody mogą zwracać nazwy widoków (HTML) lub przekierowania
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ModelAttribute;

@Controller // <-- ZMIANA 1
public class AppController {

    private final PersonService personService;

    // Konstruktor do wstrzykiwania serwisu (tak jak było)
    public AppController(PersonService personService) {
        this.personService = personService;
    }

    /**
     * Ta metoda poprawnie zapisuje nową osobę,
     * a następnie przekierowuje przeglądarkę z powrotem na stronę główną.
     */
    @PostMapping("/add-person")
    // ZMIANA 2: Zmieniamy typ zwracany na String
    public String addPerson(@ModelAttribute PersonRequest personRequest) {

        // 1. Zapisz osobę (to już działa)
        personService.savePerson(personRequest.getName());

        // 2. ZMIANA 3: Zwracamy "redirect:/"
        // To jest polecenie dla przeglądarki: "Natychmiast przejdź na adres /"
        return "redirect:/";
    }

    // Usunąłem metodę @GetMapping("/hello"), ponieważ w @Controller
    // powodowałaby błąd (próbowałaby znaleźć widok o nazwie "Hello...!")
    // Jeśli jej potrzebujesz, musisz dodać jej adnotację @ResponseBody
}