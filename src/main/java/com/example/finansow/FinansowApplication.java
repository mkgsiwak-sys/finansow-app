package com.example.finansow;

// <<< USUNIĘTE: Importy SDK Tuya >>>
// import com.tuya.cloud.openapi.springboot.annotation.ConnectorScan;

import com.example.finansow.config.TuyaConfig; // <<< DODANE: Import nowej klasy konfiguracyjnej
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties; // <<< DODANE
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(TuyaConfig.class) // <<< DODANE: Włączanie konfiguracji Tuya
// <<< USUNIĘTE: Adnotacja @ConnectorScan Tuya SDK >>>
// @ConnectorScan(basePackages = "com.tuya.cloud.openapi.service")

public class FinansowApplication {

    public static void main(String[] args) {
        SpringApplication.run(FinansowApplication.class, args);
    }

}