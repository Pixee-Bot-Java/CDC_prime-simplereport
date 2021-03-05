package gov.cdc.usds.simplereport.api.pxp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import gov.cdc.usds.simplereport.db.model.Facility;
import gov.cdc.usds.simplereport.db.model.Organization;
import gov.cdc.usds.simplereport.db.model.PatientLink;
import gov.cdc.usds.simplereport.db.model.Person;
import gov.cdc.usds.simplereport.db.model.TestOrder;
import gov.cdc.usds.simplereport.db.model.TimeOfConsent;
import gov.cdc.usds.simplereport.db.model.auxiliary.AskOnEntrySurvey;
import gov.cdc.usds.simplereport.service.TimeOfConsentService;
import gov.cdc.usds.simplereport.test_util.DbTruncator;
import gov.cdc.usds.simplereport.test_util.TestDataFactory;
import gov.cdc.usds.simplereport.test_util.TestIdentityConfiguration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class PatientExperienceControllerTest {
  @Autowired private MockMvc _mockMvc;

  @Autowired private TestDataFactory _dataFactory;

  @Autowired private TimeOfConsentService _tocService;

  @Autowired private PatientExperienceController _controller;

  @Autowired private DbTruncator _truncator;

  private Organization _org;
  private Facility _site;

  @BeforeEach
  void init() {
    _truncator.truncateAll();
    TestIdentityConfiguration.withStandardUser(
        () -> {
          _org = _dataFactory.createValidOrg();
          _site = _dataFactory.createValidFacility(_org);
          _person = _dataFactory.createFullPerson(_org);
          _testOrder = _dataFactory.createTestOrder(_person, _site);
          _patientLink = _dataFactory.createPatientLink(_testOrder);
        });
  }

  private Person _person;
  private PatientLink _patientLink;
  private TestOrder _testOrder;

  @Test
  void contextLoads() throws Exception {
    assertThat(_controller).isNotNull();
  }

  @Test
  void preAuthorizerThrows403() throws Exception {
    String dob = "1900-01-01";
    String requestBody =
        "{\"patientLinkId\":\"" + UUID.randomUUID() + "\",\"dateOfBirth\":\"" + dob + "\"}";

    MockHttpServletRequestBuilder builder =
        put("/pxp/link/verify")
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .accept(MediaType.APPLICATION_JSON)
            .characterEncoding("UTF-8")
            .content(requestBody);

    this._mockMvc.perform(builder).andExpect(status().isForbidden());
  }

  @Test
  void preAuthorizerSucceeds() throws Exception {
    // GIVEN
    String dob = _person.getBirthDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    String requestBody =
        "{\"patientLinkId\":\""
            + _patientLink.getInternalId()
            + "\",\"dateOfBirth\":\""
            + dob
            + "\"}";

    // WHEN
    MockHttpServletRequestBuilder builder =
        put("/pxp/link/verify")
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .accept(MediaType.APPLICATION_JSON)
            .characterEncoding("UTF-8")
            .content(requestBody);

    // THEN
    this._mockMvc.perform(builder).andExpect(status().isOk());
  }

  @Test
  void verifyLinkReturnsPerson() throws Exception {
    // GIVEN
    String dob = _person.getBirthDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    String requestBody =
        "{\"patientLinkId\":\""
            + _patientLink.getInternalId()
            + "\",\"dateOfBirth\":\""
            + dob
            + "\"}";

    // WHEN
    MockHttpServletRequestBuilder builder =
        put("/pxp/link/verify")
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .accept(MediaType.APPLICATION_JSON)
            .characterEncoding("UTF-8")
            .content(requestBody);

    // THEN
    _mockMvc
        .perform(builder)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.firstName", is(_person.getFirstName())))
        .andExpect(jsonPath("$.lastName", is(_person.getLastName())));
  }

  @Test
  void verifyLinkSavesTimeOfConsent() throws Exception {
    // GIVEN
    String dob = _person.getBirthDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    String requestBody =
        "{\"patientLinkId\":\""
            + _patientLink.getInternalId()
            + "\",\"dateOfBirth\":\""
            + dob
            + "\"}";

    // WHEN
    MockHttpServletRequestBuilder builder =
        put("/pxp/link/verify")
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .accept(MediaType.APPLICATION_JSON)
            .characterEncoding("UTF-8")
            .content(requestBody);

    // THEN
    _mockMvc.perform(builder).andExpect(status().isOk());
    List<TimeOfConsent> tocList = _tocService.getTimeOfConsent(_patientLink);
    assertNotNull(tocList);
    assertNotEquals(tocList.size(), 0);
  }

  @Test
  void updatePatientReturnsUpdatedPerson() throws Exception {
    // GIVEN
    String dob = _person.getBirthDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    String newFirstName = "Blob";
    String newLastName = "McBlobster";

    String requestBody =
        "{\"patientLinkId\":\""
            + _patientLink.getInternalId()
            + "\",\"dateOfBirth\":\""
            + dob
            + "\",\"data\":{\"firstName\":\""
            + newFirstName
            + "\",\"middleName\":null,\"lastName\":\""
            + newLastName
            + "\",\"birthDate\":\"0101-01-01\",\"telephone\":\"123-123-1234\",\"role\":\"UNKNOWN\",\"email\":null,\"race\":\"refused\",\"ethnicity\":\"not_hispanic\",\"gender\":\"female\",\"residentCongregateSetting\":false,\"employedInHealthcare\":true,\"address\":{\"street\":[\"12 Someplace\",\"CA\"],\"city\":null,\"state\":\"CA\",\"county\":null,\"zipCode\":\"67890\"}}}";

    // WHEN
    MockHttpServletRequestBuilder builder =
        put("/pxp/patient")
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .accept(MediaType.APPLICATION_JSON)
            .characterEncoding("UTF-8")
            .content(requestBody);

    // THEN
    _mockMvc
        .perform(builder)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.firstName", is(newFirstName)))
        .andExpect(jsonPath("$.lastName", is(newLastName)));
  }

  @Test
  void aoeSubmitCallsUpdate() throws Exception {
    // GIVEN
    String dob = _person.getBirthDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    boolean noSymptoms = false;
    String symptomOnsetDate = "2021-02-01";
    String requestBody =
        "{\"patientLinkId\":\""
            + _patientLink.getInternalId()
            + "\",\"dateOfBirth\":\""
            + dob
            + "\",\"data\":{\"noSymptoms\":"
            + noSymptoms
            + ",\"symptoms\":\"{\\\"25064002\\\":false,\\\"36955009\\\":false,\\\"43724002\\\":false,\\\"44169009\\\":false,\\\"49727002\\\":false,\\\"62315008\\\":false,\\\"64531003\\\":true,\\\"68235000\\\":false,\\\"68962001\\\":false,\\\"84229001\\\":true,\\\"103001002\\\":false,\\\"162397003\\\":false,\\\"230145002\\\":false,\\\"267036007\\\":false,\\\"422400008\\\":false,\\\"422587007\\\":false,\\\"426000000\\\":false}\",\"symptomOnset\":\""
            + symptomOnsetDate
            + "\",\"firstTest\":true,\"priorTestDate\":null,\"priorTestType\":null,\"priorTestResult\":null,\"pregnancy\":\"261665006\"}}";

    // WHEN
    MockHttpServletRequestBuilder builder =
        put("/pxp/questions")
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .accept(MediaType.APPLICATION_JSON)
            .characterEncoding("UTF-8")
            .content(requestBody);

    // THEN
    _mockMvc.perform(builder).andExpect(status().isOk());

    AskOnEntrySurvey survey = _dataFactory.getAoESurveyForTestOrder(_testOrder.getInternalId());
    assertEquals(survey.getNoSymptoms(), noSymptoms);
    assertEquals(survey.getSymptomOnsetDate(), LocalDate.parse(symptomOnsetDate));
  }
}
