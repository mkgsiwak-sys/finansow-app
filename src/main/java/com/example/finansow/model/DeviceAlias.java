package com.example.finansow.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "device_alias")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeviceAlias {

    /**
     * Używamy Device ID z Tuya jako klucza głównego.
     */
    @Id
    private String deviceId;

    /**
     * Własna, przyjazna nazwa ustawiona przez użytkownika.
     */
    private String customName;
}