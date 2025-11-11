package com.example.finansow.service;

import com.example.finansow.model.Person;
import com.example.finansow.repository.PersonRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PersonService {

    private final PersonRepository personRepository;

    public PersonService(PersonRepository personRepository) {
        this.personRepository = personRepository;
    }

    /**
     * ZMIANA: Usunęliśmy blok try...catch.
     * Jeśli teraz zapis się nie uda, metoda rzuci wyjątek,
     * co spowoduje błąd 500 i pokaże nam prawdziwy problem w konsoli.
     */
    @Transactional
    public void savePerson(String name) {

        Person newPerson = new Person();
        newPerson.setName(name);

        // Ta linia teraz rzuci błędem, jeśli coś jest nie tak
        personRepository.save(newPerson);

        System.out.println("[PersonService] Pomyślnie zapisano w bazie: " + name);
    }

    // Ta metoda będzie potrzebna do wyświetlania listy osób
    // (zakładając, że Twój główny kontroler jej jeszcze nie ma)
    public java.util.List<Person> getAllPeople() {
        return personRepository.findAll();
    }
}