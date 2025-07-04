package com.julianhaeh.hospital.system.entities;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(name = "registration")
public class RegistrationEntity {

    @EmbeddedId
    private RegistrationId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("patientId")
    @JoinColumn(name = "patient_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private PatientEntity patient;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("hospitalId")
    @JoinColumn(name = "hospital_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private HospitalEntity hospital;

    @Column(name = "registered_at", nullable = false)
    private LocalDateTime registeredAt;

    public RegistrationEntity() {}

    public RegistrationEntity(PatientEntity patient, HospitalEntity hospital) {
        this.patient      = patient;
        this.hospital     = hospital;
        this.id           = new RegistrationId(patient.getId(), hospital.getId());
        this.registeredAt = LocalDateTime.now();
    }

    public RegistrationId getId() {
        return id;
    }

    public PatientEntity getPatient() {
        return patient;
    }

    public void setPatient(PatientEntity patient) {
        this.patient = patient;
    }

    public HospitalEntity getHospital() {
        return hospital;
    }

    public void setHospital(HospitalEntity hospital) {
        this.hospital = hospital;
    }

    public LocalDateTime getRegisteredAt() {
        return registeredAt;
    }

    public void setRegisteredAt(LocalDateTime registeredAt) {
        this.registeredAt = registeredAt;
    }

    // --- Embedded-ID-Klasse ---

    @Embeddable
    public static class RegistrationId implements Serializable {

        @Column(name = "patient_id")
        private Long patientId;

        @Column(name = "hospital_id")
        private Long hospitalId;

        public RegistrationId() {}

        public RegistrationId(Long patientId, Long hospitalId) {
            this.patientId  = patientId;
            this.hospitalId = hospitalId;
        }

        // --- equals & hashCode must include both keys ---

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof RegistrationId)) return false;
            RegistrationId that = (RegistrationId) o;
            return Objects.equals(patientId, that.patientId) &&
                   Objects.equals(hospitalId, that.hospitalId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(patientId, hospitalId);
        }

        public Long getPatientId() {
            return patientId;
        }

        public void setPatientId(Long patientId) {
            this.patientId = patientId;
        }

        public Long getHospitalId() {
            return hospitalId;
        }

        public void setHospitalId(Long hospitalId) {
            this.hospitalId = hospitalId;
        }
    }
}
