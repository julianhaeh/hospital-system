package com.julianhaeh.hospital.system;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import com.google.protobuf.Empty;
import com.julianhaeh.hospital.system.entities.*;
import com.julianhaeh.hospital.system.entities.RegistrationEntity.RegistrationId;
import com.julianhaeh.hospital.system.grpc.*;
import com.julianhaeh.hospital.system.repository.*;

import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Integrationtest for HospitalServiceImpl
 */
@SpringBootTest
public class HospitalServiceIntegrationTest {

    @Autowired
    private HospitalRepository hospitalRepo;

	@Autowired
	private PatientRepository patientRepo;

	@Autowired
	private RegistrationRepository registrationRepo;

    @Autowired
    private HospitalServiceImpl service;

    // Streamobserver, which collects a single response
    static class SingleResponseObserver<T> implements StreamObserver<T> {
        private final BlockingQueue<Object> queue = new ArrayBlockingQueue<>(1);

        @Override
        public void onNext(T value) {
            queue.offer(value);
        }

        @Override
        public void onError(Throwable t) {
            queue.offer(t);
        }

        @Override
        public void onCompleted() {
			// Nothing to do here, we only care about the response or error
        }

        @SuppressWarnings("unchecked")
        T getResponse(long timeoutSeconds) throws Throwable {
            Object o = queue.poll(timeoutSeconds, TimeUnit.SECONDS);
    		if (o instanceof Throwable) {
       		throw new RuntimeException((Throwable) o);
   			}
            return (T)o;
        }
    }

    @BeforeEach
    void cleanup() {
        registrationRepo.deleteAll();
    	patientRepo.deleteAll();
    	hospitalRepo.deleteAll();
    }

    @Test
    void createModifyDeleteFlow() throws Throwable {
        // --- 1) CreateHospital ---
        CreateHospitalRequest createReq = CreateHospitalRequest.newBuilder()
            .setName("Testklinik")
            .setAddress("Musterweg 1")
            .build();
        SingleResponseObserver<Hospital> createObs = new SingleResponseObserver<>();
        service.createHospital(createReq, createObs);

        Hospital createResp = createObs.getResponse(1);
        Long Id = createResp.getId();
        assertThat(Id).isNotNull();
        assertThat(hospitalRepo.existsById(Id)).isTrue();
		HospitalEntity fromDb = hospitalRepo.findById(Id).orElseThrow();
    	assertThat(fromDb.getName()).isEqualTo("Testklinik");
    	assertThat(fromDb.getAddress()).isEqualTo("Musterweg 1");

        // --- 2) ModifyHospital ---
        ModifyHospitalRequest modifyReq = ModifyHospitalRequest.newBuilder()
            .setHospitalId(Id)
            .setName("Neue Klinik")
            .setAddress("Neuer Weg 2")
            .build();
        SingleResponseObserver<Hospital> modifyObs = new SingleResponseObserver<>();
        service.modifyHospital(modifyReq, modifyObs);

        Hospital modifyResp = modifyObs.getResponse(1);
        assertThat(modifyResp.getId()).isEqualTo(Id);
        assertThat(modifyResp.getName()).isEqualTo("Neue Klinik");
        assertThat(modifyResp.getAddress()).isEqualTo("Neuer Weg 2");
        fromDb = hospitalRepo.findById(Id).orElseThrow();
        assertThat(fromDb.getName()).isEqualTo("Neue Klinik");
		assertThat(fromDb.getAddress()).isEqualTo("Neuer Weg 2");

        // --- 3) DeleteHospital ---
        DeleteHospitalRequest deleteReq = DeleteHospitalRequest.newBuilder()
            .setHospitalId(Id)
            .build();
        SingleResponseObserver<Empty> deleteObs = new SingleResponseObserver<>();
        service.deleteHospital(deleteReq, deleteObs);

        Empty deleteResp = deleteObs.getResponse(1);
        assertThat(deleteResp).isNotNull();
        assertThat(hospitalRepo.existsById(Id)).isFalse();
    }

    @Test
    void deleteNonExistingGivesNotFound() throws Exception {
    SingleResponseObserver<Empty> obs = new SingleResponseObserver<>();
    service.deleteHospital(
            DeleteHospitalRequest.newBuilder().setHospitalId(9999).build(),
            obs
        );
        
	// Expect a RuntimeException to be thrown, which wraps the StatusRuntimeException
    RuntimeException thrown = assertThrows(
        RuntimeException.class,
        () -> obs.getResponse(1),
        "Expected a wrapped StatusRuntimeException"
    );

	// Check that the cause is a StatusRuntimeException with NOT_FOUND status
    Throwable cause = thrown.getCause();
    assertThat(cause)
        .isInstanceOf(io.grpc.StatusRuntimeException.class)
        .hasMessageContaining("NOT_FOUND");
    }

	@Test
	public void RegistrationUnregistrationFlow() throws Throwable {
		// --- 1) CreateHospital ---
		CreateHospitalRequest createReq = CreateHospitalRequest.newBuilder()
			.setName("Testklinik")
			.setAddress("Musterweg 1")
			.build();
		SingleResponseObserver<Hospital> createObs = new SingleResponseObserver<>();
		service.createHospital(createReq, createObs);
		Hospital createResp = createObs.getResponse(1);
		Long hospitalId = createResp.getId();
		HospitalEntity hFromDb = hospitalRepo.findById(hospitalId).orElseThrow();
		assertThat(hFromDb.getName()).isEqualTo("Testklinik");
		assertThat(hFromDb.getAddress()).isEqualTo("Musterweg 1");

		assertThat(hospitalRepo.existsById(hospitalId)).isTrue();

		// --- 2) CreatePatient ---
		CreatePatientRequest patientReq = CreatePatientRequest.newBuilder()
			.setFirstName("Max")
			.setLastName("Mustermann")
			.setBirthDate(LocalDate.of(1990, 1, 1).toString())
			.build();
		SingleResponseObserver<Patient> patientObs = new SingleResponseObserver<>();
		service.createPatient(patientReq, patientObs);
		Patient patientResp = patientObs.getResponse(1);
		Long patientId = patientResp.getId();

		assertThat(patientRepo.existsById(patientId)).isTrue();
		PatientEntity pFromDb = patientRepo.findById(patientId).orElseThrow();
		assertThat(pFromDb.getFirstName()).isEqualTo("Max");
		assertThat(pFromDb.getLastName()).isEqualTo("Mustermann");
		assertThat(pFromDb.getBirthDate()).isEqualTo(LocalDate.of(1990, 1, 1));

		// --- 3) Register Patient ---
		RegisterPatientRequest regReq = RegisterPatientRequest.newBuilder()
			.setPatientId(patientId)
			.setHospitalId(hospitalId)
			.build();
		SingleResponseObserver<Registration> regObs = new SingleResponseObserver<>();
		service.registerPatient(regReq, regObs);

		Registration regResp = regObs.getResponse(1);
		assertThat(regResp).isNotNull();
		
		RegistrationId regId = new RegistrationId(patientId, hospitalId);
		assertThat(registrationRepo.existsById(regId)).isTrue();
		RegistrationEntity regFromDb = registrationRepo.findById(regId).orElseThrow();
		assertThat(regFromDb.getId().getPatientId()).isEqualTo(patientId);
		assertThat(regFromDb.getId().getHospitalId()).isEqualTo(hospitalId);

		// --- 4) Unregister Patient ---
		RegisterPatientRequest unregReq = RegisterPatientRequest.newBuilder()
			.setPatientId(patientId)
			.setHospitalId(hospitalId)
			.build();
		SingleResponseObserver<Empty> unregObs = new SingleResponseObserver<>();
		service.unregisterPatient(unregReq, unregObs);
		Empty unregResp = unregObs.getResponse(1);
		assertThat(unregResp).isNotNull(); 
		
		assertThat(registrationRepo.existsById(regId)).isFalse();

		// --- 5) Patient and Hospital still exist ---
		assertThat(patientRepo.existsById(patientId)).isTrue();
		assertThat(hospitalRepo.existsById(hospitalId)).isTrue();
		}

		@Test
		public void testListOfPatientsOfHospital() throws Throwable {
			// --- 1) CreateHospital ---
			CreateHospitalRequest createReq = CreateHospitalRequest.newBuilder()
				.setName("Testklinik")
				.setAddress("Musterweg 1")
				.build();
			SingleResponseObserver<Hospital> createObs = new SingleResponseObserver<>();
			service.createHospital(createReq, createObs);
			Hospital createResp = createObs.getResponse(1);
			Long hospitalId = createResp.getId();

			assertThat(hospitalRepo.existsById(hospitalId)).isTrue();

			// --- 2) CreatePatient ---
			CreatePatientRequest patientReq = CreatePatientRequest.newBuilder()
				.setFirstName("Max")
				.setLastName("Mustermann")
				.setBirthDate(LocalDate.of(1990, 1, 1).toString())
				.build();
			SingleResponseObserver<Patient> patientObs = new SingleResponseObserver<>();
			service.createPatient(patientReq, patientObs);
			Patient patientResp = patientObs.getResponse(1);
			Long patientId = patientResp.getId();

			assertThat(patientRepo.existsById(patientId)).isTrue();

			// --- 3) Register Patient in Hospital ---
			RegisterPatientRequest regReq = RegisterPatientRequest.newBuilder()
				.setPatientId(patientId)
				.setHospitalId(hospitalId)
				.build();
			SingleResponseObserver<Registration> regObs = new SingleResponseObserver<>();
			service.registerPatient(regReq, regObs);

			Registration regResp = regObs.getResponse(1);
			RegistrationId regId = new RegistrationId(patientId, hospitalId);
			assertThat(regResp.getHospitalId()).isEqualTo(hospitalId);
			assertThat(regResp.getPatientId()).isEqualTo(patientId);
			assertThat(registrationRepo.existsById(regId)).isTrue();

			// --- 4) List Patients of Hospital ---
			ListPatientsRequest listReq = ListPatientsRequest.newBuilder()
				.setHospitalId(hospitalId)
				.build();
			SingleResponseObserver<PatientList> listObs = new SingleResponseObserver<>();
			service.listPatientsOfHospital(listReq, listObs);

			PatientList listResp = listObs.getResponse(1);
			assertThat(listResp.getPatientsCount()).isEqualTo(1);
			
			assertThat(listResp.getPatients(0).getFirstName()).isEqualTo("Max");
			assertThat(listResp.getPatients(0).getLastName()).isEqualTo("Mustermann");
			assertThat(listResp.getPatients(0).getBirthDate()).isEqualTo("1990-01-01");
	}

	@Test
	public void testListOfHospitalsOfPatient() throws Throwable {
		// --- 1) Create Hospital ---
		CreateHospitalRequest createReq = CreateHospitalRequest.newBuilder()
			.setName("Testklinik")
			.setAddress("Musterweg 1")
			.build();
		SingleResponseObserver<Hospital> createObs = new SingleResponseObserver<>();
		service.createHospital(createReq, createObs);
		Hospital createResp = createObs.getResponse(1);
		Long hospitalId = createResp.getId();

		assertThat(hospitalRepo.existsById(hospitalId)).isTrue();

		// --- 2) Create Patient ---
		CreatePatientRequest patientReq = CreatePatientRequest.newBuilder()
			.setFirstName("Max")
			.setLastName("Mustermann")
			.setBirthDate(LocalDate.of(1990, 1, 1).toString())
			.build();
		SingleResponseObserver<Patient> patientObs = new SingleResponseObserver<>();
		service.createPatient(patientReq, patientObs);
		Patient patientResp = patientObs.getResponse(1);
		Long patientId = patientResp.getId();

		assertThat(patientRepo.existsById(patientId)).isTrue();

		// --- 3) Register Patient in Hospital ---
		RegisterPatientRequest regReq = RegisterPatientRequest.newBuilder()
			.setPatientId(patientId)
			.setHospitalId(hospitalId)
			.build();
		SingleResponseObserver<Registration> regObs = new SingleResponseObserver<>();
		service.registerPatient(regReq, regObs);

		Registration regResp = regObs.getResponse(1);
		assertThat(regResp).isNotNull();
		
		RegistrationId regId = new RegistrationEntity.RegistrationId(patientId, hospitalId);
		assertThat(registrationRepo.existsById(regId)).isTrue();
		// --- 4) List Hospitals of Patient ---
		ListHospitalsRequest listReq = ListHospitalsRequest.newBuilder()
			.setPatientId(patientId)
			.build();
		SingleResponseObserver<HospitalList> listObs = new SingleResponseObserver<>();
		service.listHospitalsOfPatient(listReq, listObs);

		HospitalList listResp = listObs.getResponse(1);
		assertThat(listResp.getHospitalsCount()).isEqualTo(1);
		
		assertThat(listResp.getHospitals(0).getName()).isEqualTo("Testklinik"); 
		assertThat(listResp.getHospitals(0).getAddress()).isEqualTo("Musterweg 1");
		assertThat(listResp.getHospitals(0).getId()).isEqualTo(hospitalId);
	}

	@Test
	public void PatientNotDeletedIfHospitalDeleted() throws Throwable {
		// --- 1) Create Hospital ---
		CreateHospitalRequest createReq = CreateHospitalRequest.newBuilder()
			.setName("Testklinik")
			.setAddress("Musterweg 1")
			.build();
		SingleResponseObserver<Hospital> createObs = new SingleResponseObserver<>();
		service.createHospital(createReq, createObs);
		Hospital createResp = createObs.getResponse(1);
		Long hospitalId = createResp.getId();

		assertThat(hospitalRepo.existsById(hospitalId)).isTrue();

		// --- 2) Create Patient ---
		CreatePatientRequest patientReq = CreatePatientRequest.newBuilder()
			.setFirstName("Max")
			.setLastName("Mustermann")
			.setBirthDate(LocalDate.of(1990, 1, 1).toString())
			.build();
		SingleResponseObserver<Patient> patientObs = new SingleResponseObserver<>();
		service.createPatient(patientReq, patientObs);
		Patient patientResp = patientObs.getResponse(1);
		Long patientId = patientResp.getId();

		assertThat(patientRepo.existsById(patientId)).isTrue();

		// --- 3) Register Patient in Hospital ---
		RegisterPatientRequest regReq = RegisterPatientRequest.newBuilder()
			.setPatientId(patientId)
			.setHospitalId(hospitalId)
			.build();
		SingleResponseObserver<Registration> regObs = new SingleResponseObserver<>();
		service.registerPatient(regReq, regObs);

		assertThat(registrationRepo.existsById(new RegistrationEntity.RegistrationId(patientId, hospitalId))).isTrue();

		// --- 4) Delete Hospital ---
		DeleteHospitalRequest deleteReq = DeleteHospitalRequest.newBuilder()
			.setHospitalId(hospitalId)
			.build();
		SingleResponseObserver<Empty> deleteObs = new SingleResponseObserver<>();
		service.deleteHospital(deleteReq, deleteObs);

		Empty deleteResp = deleteObs.getResponse(1);
		assertThat(deleteResp).isNotNull();

		assertThat(hospitalRepo.existsById(hospitalId)).isFalse();
		assertThat(patientRepo.existsById(patientId)).isTrue();
	}
}

