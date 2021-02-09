package gov.cdc.usds.simplereport.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import gov.cdc.usds.simplereport.api.model.errors.IllegalGraphqlArgumentException;
import gov.cdc.usds.simplereport.db.model.Organization;
import gov.cdc.usds.simplereport.db.model.PatientLink;
import gov.cdc.usds.simplereport.db.model.Person;
import gov.cdc.usds.simplereport.db.model.TestOrder;
import gov.cdc.usds.simplereport.db.repository.PatientLinkRepository;
import gov.cdc.usds.simplereport.db.repository.TestOrderRepository;

@Service
@Transactional(readOnly = false)
public class PatientLinkService {
    @Autowired
    private PatientLinkRepository plrepo;

    @Autowired
    private TestOrderRepository torepo;

    public static final long oneDay = 24L;

    private PatientLink getPatientLink(String internalId) {
        UUID actualId = UUID.fromString(internalId);
        return plrepo.findById(actualId)
                .orElseThrow(() -> new IllegalGraphqlArgumentException("No patient link with that ID was found"));
    }

    public Organization getPatientLinkCurrent(String internalId) {
        PatientLink pl = getPatientLink(internalId);

        if (pl.getRefreshedAt().after(Date.from(Instant.now().minus(oneDay, ChronoUnit.HOURS)))) {
            return pl.getTestOrder().getOrganization();
        } else {
            return null;
        }
    }

    public Person getPatientLinkVerify(String internalId, LocalDate birthDate) throws Exception {
        PatientLink pl = getPatientLink(internalId);
        Person patient = pl.getTestOrder().getPatient();
        if (patient.getBirthDate().equals(birthDate)) {
            return patient;
        } else {
            throw new Exception("Incorrect birth date");
        }
    }

    public PatientLink createPatientLink(UUID testOrderUuid) {
        TestOrder to = torepo.findById(testOrderUuid)
                .orElseThrow(() -> new IllegalGraphqlArgumentException("No test order with that ID was found"));
        PatientLink pl = new PatientLink(to);
        return plrepo.save(pl);
    }

    public PatientLink refreshPatientLink(String internalId) {
        PatientLink pl = getPatientLink(internalId);
        pl.refresh();
        return plrepo.save(pl);
    }
}
