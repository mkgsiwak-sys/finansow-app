package com.example.finansow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FinansowApplication { // <-- TA NAZWA MUSI PASOWAĆ DO NAZWY PLIKU

    public static void main(String[] args) {
        SpringApplication.run(FinansowApplication.class, args);
    }

}

