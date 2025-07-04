package com.julianhaeh.hospital.system.grpc;

import java.time.LocalDate;
import java.util.Optional;
import java.util.List;

import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

import com.google.protobuf.Empty;
import com.julianhaeh.hospital.system.entities.*;
import com.julianhaeh.hospital.system.repository.*;

@GrpcService
public class HospitalServiceImpl extends HospitalServiceGrpc.HospitalServiceImplBase {

    private final HospitalRepository hospitalRepo;
    private final PatientRepository patientRepo;
    private final RegistrationRepository registrationRepo;

    public HospitalServiceImpl(HospitalRepository hospitalRepo,
                               PatientRepository  patientRepo,
                               RegistrationRepository registrationRepo) {
        this.hospitalRepo     = hospitalRepo;
        this.patientRepo      = patientRepo;
        this.registrationRepo = registrationRepo;
    }

    @Override
    public void createHospital(CreateHospitalRequest req,
                                  StreamObserver<Hospital> resp) {
        HospitalEntity h = new HospitalEntity();
        h.setName(req.getName());
        h.setAddress(req.getAddress());
        HospitalEntity saved = hospitalRepo.save(h);
        Hospital reply = Hospital.newBuilder()
            .setName(saved.getName())
            .setAddress(saved.getAddress())
            .setId(saved.getId())
            .build();
        resp.onNext(reply);
        resp.onCompleted();
    }

   @Override
    public void deleteHospital(DeleteHospitalRequest req, 
                                 StreamObserver<Empty> resp) {
    long id = req.getHospitalId();
    if (hospitalRepo.existsById(id)) {
        hospitalRepo.deleteById(id);
        resp.onNext(Empty.newBuilder().build());
    } else {
        resp.onError(io.grpc.Status.NOT_FOUND
            .withDescription("Hospital not found with id: " + id)
            .asRuntimeException());
        return;
            }
    resp.onCompleted();
    }   

    @Override
    public void modifyHospital(ModifyHospitalRequest req,
                                  StreamObserver<Hospital> resp){
    long id = req.getHospitalId();
    Optional<HospitalEntity> optional = hospitalRepo.findById(id);
    if (optional.isPresent()) {
    HospitalEntity h = optional.get();
    h.setName(req.getName());
    h.setAddress(req.getAddress());
    HospitalEntity saved = hospitalRepo.save(h);
        Hospital reply = Hospital.newBuilder()
            .setId(saved.getId())
            .setAddress(saved.getAddress())
            .setName(saved.getName()).
            build();
    resp.onNext(reply);
    } else {
        resp.onError(io.grpc.Status.NOT_FOUND
            .withDescription("Hospital not found with id: " + id)
            .asRuntimeException());
        return;
    }
    resp.onCompleted();
    }

    @Override
    public void createPatient(CreatePatientRequest req,
                                 StreamObserver<Patient> resp){
    PatientEntity p = new PatientEntity();
    p.setFirstName(req.getFirstName());
    p.setLastName(req.getLastName());
    p.setBirthDate(LocalDate.parse(req.getBirthDate()));
    PatientEntity saved = patientRepo.save(p);
    Patient reply = Patient.newBuilder()
            .setFirstName(saved.getFirstName())
            .setLastName(saved.getLastName())
            .setId(saved.getId())
            .setBirthDate(saved.getBirthDate().toString())
            .build();
    resp.onNext(reply);
    resp.onCompleted();
    }

    @Override
    public void modifyPatient(ModifyPatientRequest req,
                                StreamObserver<Patient> resp){
    long id = req.getPatientId();
    Optional<PatientEntity> optional = patientRepo.findById(id);
    if (optional.isPresent()) {
    PatientEntity p = optional.get();
    p.setFirstName(req.getFirstName());
    p.setLastName(req.getLastName());
    p.setBirthDate(LocalDate.parse(req.getBirthDate()));
    PatientEntity saved = patientRepo.save(p);
        Patient reply = Patient.newBuilder()
            .setId(saved.getId())
            .setFirstName(saved.getFirstName())
            .setLastName(saved.getLastName()).
            build();
    resp.onNext(reply);
    } else {
        resp.onError(io.grpc.Status.NOT_FOUND
            .withDescription("Patient not found with id: " + id)
            .asRuntimeException());
        return;
    }
    resp.onCompleted();
    }

    @Override
    public void deletePatient(DeletePatientRequest req,  
                                 StreamObserver<Empty> resp) {
        long id = req.getPatientId();
        if (patientRepo.existsById(id)) {
            patientRepo.deleteById(id);
            resp.onNext(Empty.newBuilder().build());
        } else {
            resp.onError(io.grpc.Status.NOT_FOUND
                .withDescription("Patient not found with id: " + id)
                .asRuntimeException());
            return;
        }
        resp.onCompleted();
    }

    @Override
    public void registerPatient(RegisterPatientRequest req,
                                  StreamObserver<Registration> resp) {
        long patientId = req.getPatientId();
        long hospitalId = req.getHospitalId();

        Optional<PatientEntity> patientOpt = patientRepo.findById(patientId);
        Optional<HospitalEntity> hospitalOpt = hospitalRepo.findById(hospitalId);

        if (patientOpt.isPresent() && hospitalOpt.isPresent()) {
            PatientEntity patient = patientOpt.get();
            HospitalEntity hospital = hospitalOpt.get();
            RegistrationEntity r = new RegistrationEntity(patient, hospital);
            registrationRepo.save(r);
            Registration reply = Registration.newBuilder()
                .setPatientId(patient.getId())
                .setHospitalId(hospital.getId())
                .build();
            resp.onNext(reply);
        } else {
            resp.onError(io.grpc.Status.NOT_FOUND
                .withDescription("Patient or Hospital not found")
                .asRuntimeException());
            return;
        }
        resp.onCompleted();
    }

    @Override
    public void unregisterPatient(RegisterPatientRequest req,
                                  StreamObserver<Empty> resp) {
        long patientId = req.getPatientId();
        long hospitalId = req.getHospitalId();

        RegistrationEntity.RegistrationId regId = new RegistrationEntity.RegistrationId(patientId, hospitalId);
        if (registrationRepo.existsById(regId)) {
            registrationRepo.deleteById(regId);
            resp.onNext(Empty.newBuilder().build());
        } else {
            resp.onError(io.grpc.Status.NOT_FOUND
                .withDescription("Registration not found")
                .asRuntimeException());
            return;
        }
        resp.onCompleted();
    }

    @Override
    public void listPatientsOfHospital(ListPatientsRequest req,
                                  StreamObserver<PatientList> resp) {
        long hospitalId = req.getHospitalId();
        List<RegistrationEntity> registrations = registrationRepo.findByHospitalId(hospitalId);

        PatientList.Builder responseBuilder = PatientList.newBuilder();
        for (RegistrationEntity reg : registrations) {
            PatientEntity p = patientRepo.findById(reg.getId().getPatientId()).orElseThrow();  
            Patient protoPatient = Patient.newBuilder()
                .setId(p.getId())
                .setFirstName(p.getFirstName())
                .setLastName(p.getLastName())
                .setBirthDate(p.getBirthDate().toString())
                .build();
            responseBuilder.addPatients(protoPatient);
        }

        resp.onNext(responseBuilder.build());
        resp.onCompleted();
    }

    @Override
    public void listHospitalsOfPatient(ListHospitalsRequest req,
                                  StreamObserver<HospitalList> resp) {
        long patientId = req.getPatientId();
        List<RegistrationEntity> registrations = registrationRepo.findByPatientId(patientId);
        HospitalList.Builder responseBuilder = HospitalList.newBuilder();
        for (RegistrationEntity reg : registrations) {
            HospitalEntity h = hospitalRepo.findById(reg.getId().getHospitalId()).orElseThrow();
            Hospital protoHospital = Hospital.newBuilder()
                .setId(h.getId())
                .setName(h.getName())
                .setAddress(h.getAddress())
                .build();
            responseBuilder.addHospitals(protoHospital);
        }
        resp.onNext(responseBuilder.build());
        resp.onCompleted();
    }



}

