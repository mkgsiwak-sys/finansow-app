package com.example.finansow.controller;

import com.example.finansow.model.*;
import com.example.finansow.repository.*;
import com.example.finansow.service.RecurringService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Controller
public class AppController {

    // Wszystkie repozytoria
    @Autowired private PersonRepository personRepository;
    @Autowired private TransactionRepository transactionRepository;
    @Autowired private TaskRepository taskRepository;
    @Autowired private RecurringTransactionRepository recurringTransactionRepo;
    @Autowired private RecurringTaskRepository recurringTaskRepo;
    @Autowired private RecurringService recurringService;

    private static final String[] PERSON_COLORS = {
            "bg-indigo-500", "bg-pink-500", "bg-green-500", "bg-yellow-500", "bg-red-500", "bg-blue-500"
    };

    @GetMapping("/")
    public String getIndexPage(
            @RequestParam(name = "activePersonId", required = false) Long activePersonId,
            @RequestParam(name = "month", required = false) String monthQuery, // np. "2025-10"
            Model model
    ) {
        // --- 1. Obsługa Osób ---
        List<Person> people = personRepository.findAll();
        model.addAttribute("people", people);

        Person activePerson = null;
        if (activePersonId != null) {
            activePerson = personRepository.findById(activePersonId).orElse(null);
        } else if (!people.isEmpty()) {
            activePerson = people.get(0); // Domyślnie pierwsza osoba
        }
        model.addAttribute("activePerson", activePerson);

        // --- 2. Obsługa Miesiąca ---
        YearMonth currentMonth;
        if (monthQuery != null && !monthQuery.isEmpty()) {
            try {
                currentMonth = YearMonth.parse(monthQuery);
            } catch (Exception e) {
                currentMonth = YearMonth.now();
            }
        } else {
            currentMonth = YearMonth.now();
        }
        model.addAttribute("currentMonth", currentMonth.toString()); // "2025-10"
        model.addAttribute("currentMonthName", currentMonth.getMonth().getDisplayName(TextStyle.FULL, Locale.forLanguageTag("pl")) + " " + currentMonth.getYear());
        model.addAttribute("previousMonth", currentMonth.minusMonths(1).toString());
        model.addAttribute("nextMonth", currentMonth.plusMonths(1).toString());


        // --- 3. Pobieranie Danych (Transakcje, Zadania, Reguły) ---
        if (activePerson != null) {
            LocalDate startDate = currentMonth.atDay(1);
            LocalDate endDate = currentMonth.atEndOfMonth();
            LocalDate today = LocalDate.now(); // <<< Data na dziś

            // *** 1. WYWOŁANIE GENERATORA CYKLICZNEGO PRZEZ SERWIS ***
            recurringService.generateRecurringItems(activePerson.getId(), currentMonth);

            // A. Transakcje z danego miesiąca (do bilansu)
            List<Transaction> transactions = transactionRepository.findByPersonIdAndDateBetween(activePerson.getId(), startDate, endDate);

            // B. Niezapłacone wydatki Z TERMINEM w tym miesiącu
            List<Transaction> unpaidExpenses = transactionRepository.findByPersonIdAndPaidAndDueDateBetween(activePerson.getId(), false, startDate, endDate);

            // Przeniesienie logiki sumowania z Thymeleaf do kontrolera
            BigDecimal totalUnpaid = unpaidExpenses.stream()
                    .map(Transaction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            model.addAttribute("unpaidExpenses", unpaidExpenses); // Przekaż listę
            model.addAttribute("totalUnpaidExpenses", totalUnpaid); // Przekaż obliczoną sumę

            // C. Zadania Z TERMINEM w tym miesiącu
            List<Task> tasks = taskRepository.findByAssigneeIdAndDateBetween(activePerson.getId(), startDate, endDate);

            // D. Stałe płatności (zawsze wszystkie)
            List<RecurringTransaction> recurringTransactions = recurringTransactionRepo.findByPersonId(activePerson.getId());

            // E. Cykliczne zadania (zawsze wszystkie)
            List<RecurringTask> recurringTasks = recurringTaskRepo.findByAssigneeId(activePerson.getId());

            // F. <<< ZMIANA: Pobranie zadań na dziś ORAZ zaległych >>>
            // Łączymy dwie listy: zadania na dziś + zadania zaległe (po terminie)
            List<Task> tasksForToday = taskRepository.findByAssigneeIdAndCompletedAndDate(activePerson.getId(), false, today);
            List<Task> overdueTasks = taskRepository.findByAssigneeIdAndCompletedAndDateBefore(activePerson.getId(), false, today);

            List<Task> openTasks = Stream.concat(overdueTasks.stream(), tasksForToday.stream())
                    .collect(Collectors.toList());

            // Sortowanie: Najpierw wg priorytetu (malejąco), potem wg daty (rosnąco)
            openTasks.sort(Comparator.comparing(Task::getPriority).reversed()
                    .thenComparing(Task::getDate));

            model.addAttribute("openTasks", openTasks);

            // Przekaż listy do widoku (dla formularzy i list na dole strony)
            model.addAttribute("transactions", transactions);
            model.addAttribute("tasks", tasks);
            model.addAttribute("recurringTransactions", recurringTransactions);
            model.addAttribute("recurringTasks", recurringTasks);

            // G. Obliczanie Bilansu (tylko z transakcji zaksięgowanych w danym miesiącu)
            BigDecimal income = transactions.stream()
                    .filter(t -> t.getType() != null && t.getType() == TransactionType.INCOME && t.getAmount() != null)
                    .map(Transaction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal expenses = transactions.stream()
                    .filter(t -> t.getType() != null && t.getType() == TransactionType.EXPENSE && t.getAmount() != null)
                    .map(Transaction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            model.addAttribute("monthlyIncome", income);
            model.addAttribute("monthlyExpenses", expenses);
            model.addAttribute("monthlyBalance", income.subtract(expenses));

            // *** Bilans Skumulowany (do dziś) ***
            List<Transaction> allPostedTransactions = transactionRepository.findByPersonIdAndDateBefore(activePerson.getId(), LocalDate.now().plusDays(1));
            BigDecimal cumulativeBalance = allPostedTransactions.stream()
                    .map(t -> t.getType() == TransactionType.INCOME ? t.getAmount() : t.getAmount().negate())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            model.addAttribute("cumulativeBalance", cumulativeBalance);

            // --- 4. Budowanie Kalendarza ---
            List<List<CalendarDay>> calendarGrid = buildCalendarGrid(currentMonth, tasks, unpaidExpenses, recurringTransactions, recurringTasks);
            model.addAttribute("calendarGrid", calendarGrid);
            model.addAttribute("weekDays", new String[]{"Pon", "Wt", "Śr", "Czw", "Pt", "Sob", "Niedz"});
        }

        return "index"; // Nazwa pliku index.html
    }

    // --- NOWA METODA API: Zwracanie Danych dla Modala ---
    @GetMapping("/day-events")
    @ResponseBody
    public List<DayEvent> getDayEvents(
            @RequestParam(name = "personId") Long personId,
            @RequestParam(name = "date") @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        // Używamy tego samego mechanizmu, co populateCalendarDay, ale tylko dla jednego dnia
        List<DayEvent> events = new ArrayList<>();

        // Pobierz dane dla zakresu (tylko jeden dzień)
        List<Transaction> unpaidExpenses = transactionRepository.findByPersonIdAndPaidAndDueDateBetween(personId, false, date, date);
        List<Task> tasks = taskRepository.findByAssigneeIdAndDateBetween(personId, date, date);
        List<RecurringTransaction> recurringPayments = recurringTransactionRepo.findByPersonId(personId).stream()
                .filter(rp -> rp.getDayOfMonthToGenerate() == date.getDayOfMonth())
                .collect(Collectors.toList());

        // 1. Niezapłacone Wydatki (Najwyższy Priorytet: 1)
        for (Transaction t : unpaidExpenses) {
            String details = String.format("%s PLN (Termin: %s)", t.getAmount().toString(), t.getDueDate().format(DateTimeFormatter.ISO_DATE));
            String actionUrl = String.format("/toggle-paid?id=%d&month=%s", t.getId(), YearMonth.from(date).toString());
            events.add(new DayEvent(t.getDescription(), "WYDATEK", details, "text-red-600", actionUrl, 1));
        }

        // 2. Zadania (Średni Priorytet: 2)
        for (Task t : tasks) {
            String details = t.isCompleted() ? "Ukończone" : "Do wykonania";
            String actionUrl = String.format("/toggle-completed?id=%d&month=%s", t.getId(), YearMonth.from(date).toString());
            events.add(new DayEvent(t.getDescription(), "ZADANIE", details, t.isCompleted() ? "text-gray-500" : "text-blue-600", actionUrl, 2));
        }

        // 3. Płatności Stałe (Niski Priorytet: 3)
        for (RecurringTransaction rt : recurringPayments) {
            String details = String.format("%s %s PLN (Generacja)", rt.getType().toString(), rt.getAmount().toString());
            events.add(new DayEvent(rt.getDescription(), "STAŁA PŁATNOŚĆ", details, "text-purple-600", null, 3));
        }

        events.sort(Comparator.comparingInt(DayEvent::getPriority));
        return events;
    }


    // --- Reszta metod (budowanie kalendarza i formularze) ---

    private List<List<CalendarDay>> buildCalendarGrid(YearMonth month, List<Task> tasks, List<Transaction> unpaidExpenses, List<RecurringTransaction> recurringPayments, List<RecurringTask> recurringTasks) {
        List<List<CalendarDay>> calendarGrid = new ArrayList<>();
        LocalDate firstDayOfMonth = month.atDay(1);
        int firstDayOfWeekValue = firstDayOfMonth.getDayOfWeek().getValue();

        LocalDate startDate = firstDayOfMonth.minusDays(firstDayOfWeekValue - 1);

        LocalDate currentDate = startDate;

        for (int i = 0; i < 6; i++) {
            List<CalendarDay> week = new ArrayList<>();
            for (int j = 0; j < 7; j++) {
                CalendarDay day = new CalendarDay(currentDate, currentDate.getMonth().equals(month.getMonth()));
                populateCalendarDay(day, tasks, unpaidExpenses, recurringPayments, recurringTasks);
                week.add(day);
                currentDate = currentDate.plusDays(1);
            }
            calendarGrid.add(week);

            // Optymalizacja: Usuń puste rzędy
            if (!currentDate.getMonth().equals(month.getMonth()) && i >= 3) {
                boolean weekInCurrentMonth = week.stream().anyMatch(CalendarDay::isCurrentMonth);
                if (!weekInCurrentMonth) {
                    calendarGrid.remove(calendarGrid.size() - 1);
                    break;
                }
            }
        }
        return calendarGrid;
    }

    private void populateCalendarDay(CalendarDay day, List<Task> allTasks, List<Transaction> allUnpaidExpenses, List<RecurringTransaction> allRecurringPayments, List<RecurringTask> allRecurringTasks) {
        LocalDate date = day.getDate();
        DayOfWeek dayOfWeek = date.getDayOfWeek();

        allTasks.stream()
                .filter(task -> task.getDate() != null && task.getDate().equals(date))
                .forEach(day::addTask);

        allUnpaidExpenses.stream()
                .filter(expense -> expense.getDueDate() != null && expense.getDueDate().equals(date))
                .forEach(day::addUnpaidExpense);

        allRecurringPayments.stream()
                .filter(payment -> payment.getDayOfMonthToGenerate() == date.getDayOfMonth())
                .forEach(day::addRecurringPayment);

        allRecurringTasks.stream()
                .filter(rt -> {
                    if (rt.getRecurrenceType() == RecurrenceType.DAILY) {
                        return false;
                    }
                    if (rt.getRecurrenceType() == RecurrenceType.MONTHLY) {
                        return rt.getDayOfMonth() == date.getDayOfMonth();
                    }
                    if (rt.getRecurrenceType() == RecurrenceType.WEEKLY) {
                        switch (dayOfWeek) {
                            case MONDAY: return rt.isOnMonday();
                            case TUESDAY: return rt.isOnTuesday();
                            case WEDNESDAY: return rt.isOnWednesday();
                            case THURSDAY: return rt.isOnThursday();
                            case FRIDAY: return rt.isOnFriday();
                            case SATURDAY: return rt.isOnSaturday();
                            case SUNDAY: return rt.isOnSunday();
                            default: return false;
                        }
                    }
                    return false;
                })
                .forEach(day::addRecurringTask);
    }

    // Metody do obsługi Osób
    @PostMapping("/add-person")
    public String addPerson(@RequestParam(name = "name") String name) {
        if (name != null && !name.trim().isEmpty()) {
            long count = personRepository.count();
            String color = PERSON_COLORS[(int) (count % PERSON_COLORS.length)];
            personRepository.save(new Person(name, color));
        }
        return "redirect:/";
    }

    @Transactional
    @GetMapping("/delete-person")
    public String deletePerson(@RequestParam(name = "id") Long personId) {

        // 1. Sprawdź, czy osoba istnieje
        Optional<Person> personOpt = personRepository.findById(personId);
        if (personOpt.isPresent()) {

            // 2. Usuń wszystkie powiązane dane (kolejność nie ma znaczenia dzięki @Transactional)

            // Usuń transakcje
            transactionRepository.deleteAllByPersonId(personId);

            // Usuń stałe transakcje
            recurringTransactionRepo.deleteAllByPersonId(personId);

            // Usuń zadania (gdzie jest wykonawcą LUB zleceniodawcą)
            taskRepository.deleteAllByAssigneeId(personId);
            taskRepository.deleteAllByAssignerId(personId);

            // Usuń cykliczne zadania (gdzie jest wykonawcą LUB zleceniodawcą)
            recurringTaskRepo.deleteAllByAssigneeId(personId);
            recurringTaskRepo.deleteAllByAssignerId(personId);

            // 3. Usuń samą osobę
            personRepository.deleteById(personId);
        }

        // 4. Przekieruj na stronę główną (bez aktywnej osoby)
        return "redirect:/";
    }


    // Metody do obsługi Transakcji
    @PostMapping("/add-transaction")
    public String addTransaction(
            @RequestParam(name = "personId") Long personId,
            @RequestParam(name = "description") String description,
            @RequestParam(name = "amount") BigDecimal amount,
            @RequestParam(name = "date") LocalDate date, // Data księgowania
            @RequestParam(name = "type") TransactionType type,
            @RequestParam(name = "paid", required = false) boolean paid,
            @RequestParam(name = "invoiced", required = false) boolean invoiced,
            @RequestParam(name = "invoiceNumber", required = false) String invoiceNumber,
            @RequestParam(name = "dueDate", required = false) LocalDate dueDate,
            @RequestParam(name = "month") String month
    ) {
        Optional<Person> personOpt = personRepository.findById(personId);
        if (personOpt.isPresent()) {
            Person person = personOpt.get();
            Transaction transaction = new Transaction(description, amount, date, type, person, paid, invoiced, invoiceNumber, dueDate);
            transactionRepository.save(transaction);
        }
        return "redirect:/?activePersonId=" + personId + "&month=" + month;
    }

    @GetMapping("/toggle-paid")
    public String togglePaid(@RequestParam(name = "id") Long transactionId, @RequestParam(name = "month") String month) {
        Long personId = null;
        Optional<Transaction> transactionOpt = transactionRepository.findById(transactionId);
        if (transactionOpt.isPresent()) {
            Transaction transaction = transactionOpt.get();
            transaction.setPaid(!transaction.isPaid());
            transactionRepository.save(transaction);
            personId = transaction.getPerson().getId();
        }
        return "redirect:/?activePersonId=" + personId + "&month=" + month;
    }

    // Metody do obsługi Zadań
    @PostMapping("/add-task")
    public String addTask(
            @RequestParam(name = "assignerId") Long assignerId,
            @RequestParam(name = "assigneeId") Long assigneeId,
            @RequestParam(name = "description") String description,
            @RequestParam(name = "date") LocalDate date, // Termin wykonania
            @RequestParam(name = "priority", defaultValue = "3") int priority, // <<< DODANY PARAMETR
            @RequestParam(name = "month") String month
    ) {
        Optional<Person> assignerOpt = personRepository.findById(assignerId);
        Optional<Person> assigneeOpt = personRepository.findById(assigneeId);

        if (assignerOpt.isPresent() && assigneeOpt.isPresent()) {
            Task task = new Task(description, assignerOpt.get(), assigneeOpt.get(), date, false);
            task.setPriority(priority); // <<< USTAWIANIE PRIORYTETU
            taskRepository.save(task);
        }
        return "redirect:/?activePersonId=" + assigneeId + "&month=" + month;
    }

    @GetMapping("/toggle-completed")
    public String toggleCompleted(@RequestParam(name = "id") Long taskId, @RequestParam(name = "month") String month) {
        Long personId = null;
        Optional<Task> taskOpt = taskRepository.findById(taskId);
        if (taskOpt.isPresent()) {
            Task task = taskOpt.get();
            task.setCompleted(!task.isCompleted());
            taskRepository.save(task);
            personId = task.getAssignee().getId();
        }
        return "redirect:/?activePersonId=" + personId + "&month=" + month;
    }

    // Metody do obsługi Stałych Transakcji
    @PostMapping("/add-recurring-transaction")
    public String addRecurringTransaction(
            @RequestParam(name = "personId") Long personId,
            @RequestParam(name = "description") String description,
            @RequestParam(name = "amount") BigDecimal amount,
            @RequestParam(name = "type") TransactionType type,
            @RequestParam(name = "dayOfMonthToGenerate") int dayOfMonthToGenerate,
            @RequestParam(name = "daysUntilDue") int daysUntilDue
    ) {
        Optional<Person> personOpt = personRepository.findById(personId);
        if (personOpt.isPresent()) {
            RecurringTransaction rt = new RecurringTransaction(personOpt.get(), description, amount, type, dayOfMonthToGenerate);
            rt.setDaysUntilDue(daysUntilDue);
            recurringTransactionRepo.save(rt);
        }
        return "redirect:/?activePersonId=" + personId;
    }

    @GetMapping("/delete-recurring-transaction")
    public String deleteRecurringTransaction(
            @RequestParam(name = "id") Long id,
            @RequestParam(name = "personId") Long personId
    ) {
        recurringTransactionRepo.deleteById(id);
        return "redirect:/?activePersonId=" + personId;
    }

    // Metody do obsługi Cyklicznych Zadań
    @PostMapping("/add-recurring-task")
    public String addRecurringTask(
            @RequestParam(name = "assignerId") Long assignerId,
            @RequestParam(name = "assigneeId") Long assigneeId,
            @RequestParam(name = "description") String description,
            @RequestParam(name = "recurrenceType") RecurrenceType recurrenceType,
            @RequestParam(name = "priority", defaultValue = "3") int priority, // <<< DODANY PARAMETR
            @RequestParam(name = "dayOfMonth", required = false) Integer dayOfMonth,
            @RequestParam(name = "onMonday", required = false) boolean onMonday,
            @RequestParam(name = "onTuesday", required = false) boolean onTuesday,
            @RequestParam(name = "onWednesday", required = false) boolean onWednesday,
            @RequestParam(name = "onThursday", required = false) boolean onThursday,
            @RequestParam(name = "onFriday", required = false) boolean onFriday,
            @RequestParam(name = "onSaturday", required = false) boolean onSaturday,
            @RequestParam(name = "onSunday", required = false) boolean onSunday
    ) {
        Optional<Person> assignerOpt = personRepository.findById(assignerId);
        Optional<Person> assigneeOpt = personRepository.findById(assigneeId);

        if (assignerOpt.isPresent() && assigneeOpt.isPresent()) {
            RecurringTask rt = new RecurringTask();
            rt.setAssigner(assignerOpt.get());
            rt.setAssignee(assigneeOpt.get());
            rt.setDescription(description);
            rt.setRecurrenceType(recurrenceType);
            rt.setPriority(priority); // <<< USTAWIANIE PRIORYTETU

            if (recurrenceType == RecurrenceType.MONTHLY) {
                rt.setDayOfMonth(dayOfMonth != null ? dayOfMonth : 1);
            } else {
                rt.setDayOfMonth(0);
            }

            if (recurrenceType == RecurrenceType.WEEKLY) {
                rt.setOnMonday(onMonday);
                rt.setOnTuesday(onTuesday);
                rt.setOnWednesday(onWednesday);
                rt.setOnThursday(onThursday);
                rt.setOnFriday(onFriday);
                rt.setOnSaturday(onSaturday);
                rt.setOnSunday(onSunday);
            }
            recurringTaskRepo.save(rt);
        }
        return "redirect:/?activePersonId=" + assigneeId;
    }

    @GetMapping("/delete-recurring-task")
    public String deleteRecurringTask(
            @RequestParam(name = "id") Long id,
            @RequestParam(name = "personId") Long personId
    ) {
        recurringTaskRepo.deleteById(id);
        return "redirect:/?activePersonId=" + personId;
    }

    // --- DODANE: Endpoint Archiwizacji ---
    @PostMapping("/archive-data")
    public String archiveData(@RequestParam(name = "personId") Long personId) {
        LocalDate archiveBeforeDate = LocalDate.now().minusMonths(6);

        // 1. Usuń stare Transakcje (starsze niż 6 miesięcy)
        List<Transaction> oldTransactions = transactionRepository.findByPersonIdAndDateBefore(personId, archiveBeforeDate);
        transactionRepository.deleteAll(oldTransactions);

        // 2. Usuń stare, zakończone Zadania (starsze niż 6 miesięcy)
        List<Task> oldCompletedTasks = taskRepository.findByAssigneeIdAndCompletedAndDateBefore(personId, true, archiveBeforeDate);
        taskRepository.deleteAll(oldCompletedTasks);

        return "redirect:/?activePersonId=" + personId;
    }
    // --- KONIEC ARCHIWIZACJI ---


    // Metody do obsługi Raportów CSV
    private static final DateTimeFormatter CSV_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @GetMapping("/report-monthly")
    public void getMonthlyReport(
            @RequestParam(name = "personId") Long personId,
            @RequestParam(name = "month") String month,
            HttpServletResponse response
    ) throws IOException {
        Optional<Person> personOpt = personRepository.findById(personId);
        // ... (Logika raportów)
    }

    @GetMapping("/report-yearly")
    public void getYearlyReport(
            @RequestParam(name = "personId") Long personId,
            @RequestParam(name = "year") int year,
            HttpServletResponse response
    ) throws IOException {
        // ... (Logika raportów)
    }
}