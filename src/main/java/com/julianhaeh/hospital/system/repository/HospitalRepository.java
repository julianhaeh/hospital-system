package com.julianhaeh.hospital.system.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.julianhaeh.hospital.system.entities.HospitalEntity;

public interface HospitalRepository extends JpaRepository<HospitalEntity, Long> {
}
