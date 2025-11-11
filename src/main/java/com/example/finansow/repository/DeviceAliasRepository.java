package com.example.finansow.repository;

import com.example.finansow.model.DeviceAlias;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repozytorium do zarządzania własnymi nazwami (aliasami) urządzeń Tuya.
 */
@Repository
public interface DeviceAliasRepository extends JpaRepository<DeviceAlias, String> {
}