package com.julianhaeh.hospital.system.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.julianhaeh.hospital.system.entities.PatientEntity;

public interface PatientRepository extends JpaRepository<PatientEntity, Long> {
}
