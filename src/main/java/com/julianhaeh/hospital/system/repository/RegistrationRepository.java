package com.julianhaeh.hospital.system.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.julianhaeh.hospital.system.entities.RegistrationEntity;
import com.julianhaeh.hospital.system.entities.RegistrationEntity.RegistrationId;

import java.util.List;


public interface RegistrationRepository extends JpaRepository<RegistrationEntity, RegistrationId> {

    
    List<RegistrationEntity> findByHospitalId(Long hospitalId);

    List<RegistrationEntity> findByPatientId(Long patientId);
}
