package com.julianhaeh.hospital.system.entities;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "patient")
public class PatientEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name",  nullable = false)
    private String lastName;

    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;

    public PatientEntity() {}

    public PatientEntity(String firstName, String lastName, LocalDate birthDate) {
        this.firstName = firstName;
        this.lastName  = lastName;
        this.birthDate = birthDate;
    }

    public Long getId() {
        return id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

}
