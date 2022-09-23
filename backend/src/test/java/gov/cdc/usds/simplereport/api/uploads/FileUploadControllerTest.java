package gov.cdc.usds.simplereport.api.uploads;

import static gov.cdc.usds.simplereport.api.uploads.FileUploadController.TEXT_CSV_CONTENT_TYPE;
import static gov.cdc.usds.simplereport.config.WebConfiguration.PATIENT_UPLOAD;
import static gov.cdc.usds.simplereport.config.WebConfiguration.RESULT_UPLOAD;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import gov.cdc.usds.simplereport.api.BaseFullStackTest;
import gov.cdc.usds.simplereport.api.model.errors.CsvProcessingException;
import gov.cdc.usds.simplereport.db.model.Organization;
import gov.cdc.usds.simplereport.db.model.TestResultUpload;
import gov.cdc.usds.simplereport.db.model.auxiliary.UploadStatus;
import gov.cdc.usds.simplereport.service.TestResultUploadService;
import gov.cdc.usds.simplereport.service.UploadService;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

class FileUploadControllerTest extends BaseFullStackTest {

  @Autowired private MockMvc mockMvc;
  @MockBean private UploadService patientUploadService;
  @MockBean private TestResultUploadService testResultUploadService;

  @Test
  void patientsUploadTest_happy() throws Exception {
    when(patientUploadService.processPersonCSV(any(InputStream.class)))
        .thenReturn("Successfully uploaded 1 record(s)");

    MockMultipartFile file =
        new MockMultipartFile(
            "file", "patients.csv", TEXT_CSV_CONTENT_TYPE, "csvContent".getBytes());

    mockMvc
        .perform(multipart(PATIENT_UPLOAD).file(file))
        .andExpect(status().isOk())
        .andExpect(content().string("Successfully uploaded 1 record(s)"));
  }

  @Test
  void patientsUploadTest_IllegalArgumentException() throws Exception {
    when(patientUploadService.processPersonCSV(any(InputStream.class)))
        .thenThrow(new IllegalArgumentException("Invalid csv"));

    MockMultipartFile file =
        new MockMultipartFile(
            "file", "patients.csv", TEXT_CSV_CONTENT_TYPE, "csvContent".getBytes());

    mockMvc
        .perform(multipart(PATIENT_UPLOAD).file(file))
        .andExpect(status().isBadRequest())
        .andExpect(
            result -> assertTrue(result.getResolvedException() instanceof CsvProcessingException))
        .andExpect(
            result -> assertEquals("Invalid csv", result.getResolvedException().getMessage()));
  }

  @Test
  void patientsUploadTest_NonCsvFileException() throws Exception {
    when(patientUploadService.processPersonCSV(any(InputStream.class)))
        .thenThrow(new IllegalArgumentException("Invalid csv"));

    MockMultipartFile file =
        new MockMultipartFile(
            "file", "patients.csv", MediaType.TEXT_PLAIN_VALUE, "csvContent".getBytes());

    mockMvc
        .perform(multipart(PATIENT_UPLOAD).file(file))
        .andExpect(status().isBadRequest())
        .andExpect(
            result -> assertTrue(result.getResolvedException() instanceof CsvProcessingException))
        .andExpect(
            result ->
                assertEquals(
                    "Only CSV files are supported", result.getResolvedException().getMessage()));
  }

  @Test
  void patientsUploadTest_IOException() throws Exception {
    MockMultipartFile mock = mock(MockMultipartFile.class);
    when(mock.getBytes()).thenReturn("content".getBytes());
    when(mock.getOriginalFilename()).thenReturn("patients.csv");
    when(mock.getName()).thenReturn("file");
    when(mock.getSize()).thenReturn(7L);
    when(mock.getContentType()).thenReturn(TEXT_CSV_CONTENT_TYPE);

    when(mock.getInputStream()).thenThrow(new IOException());

    mockMvc
        .perform(multipart(PATIENT_UPLOAD).file(mock))
        .andExpect(status().isBadRequest())
        .andExpect(
            result -> assertTrue(result.getResolvedException() instanceof CsvProcessingException))
        .andExpect(
            result ->
                assertEquals(
                    "Unable to complete patient CSV upload",
                    result.getResolvedException().getMessage()));
  }

  @Test
  void resultsUploadTest() throws Exception {
    UUID reportId = UUID.randomUUID();

    Organization organization = new Organization("best org", "lab", "best-org-123", true);
    TestResultUpload testResultUpload =
        new TestResultUpload(reportId, UploadStatus.SUCCESS, 5, organization, null, null);

    when(testResultUploadService.processResultCSV(any(InputStream.class)))
        .thenReturn(testResultUpload);

    MockMultipartFile file =
        new MockMultipartFile(
            "file", "results.csv", TEXT_CSV_CONTENT_TYPE, "csvContent".getBytes());

    mockMvc
        .perform(multipart(RESULT_UPLOAD).file(file))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.reportId", Matchers.is(reportId.toString())))
        .andExpect(jsonPath("$.status", Matchers.is("SUCCESS")))
        .andExpect(jsonPath("$.recordsCount", Matchers.is(5)))
        .andReturn();
  }

  @Test
  void resultsUploadTest_IOException() throws Exception {
    MockMultipartFile mock = mock(MockMultipartFile.class);
    when(mock.getBytes()).thenReturn("content".getBytes());
    when(mock.getOriginalFilename()).thenReturn("results.csv");
    when(mock.getName()).thenReturn("file");
    when(mock.getSize()).thenReturn(7L);
    when(mock.getContentType()).thenReturn(TEXT_CSV_CONTENT_TYPE);

    when(mock.getInputStream()).thenThrow(new IOException());

    mockMvc
        .perform(multipart(RESULT_UPLOAD).file(mock))
        .andExpect(status().isBadRequest())
        .andExpect(
            result -> assertTrue(result.getResolvedException() instanceof CsvProcessingException))
        .andExpect(
            result ->
                assertEquals(
                    "Unable to process test result CSV upload",
                    result.getResolvedException().getMessage()));
  }

  @Test
  void resultsUploadTest_NonCsvFileException() throws Exception {
    MockMultipartFile mock = mock(MockMultipartFile.class);
    when(mock.getBytes()).thenReturn("content".getBytes());
    when(mock.getOriginalFilename()).thenReturn("results.csv");
    when(mock.getName()).thenReturn("file");
    when(mock.getSize()).thenReturn(7L);
    when(mock.getContentType()).thenReturn(MediaType.TEXT_PLAIN_VALUE);

    when(mock.getInputStream()).thenThrow(new IOException());

    mockMvc
        .perform(multipart(RESULT_UPLOAD).file(mock))
        .andExpect(status().isBadRequest())
        .andExpect(
            result -> assertTrue(result.getResolvedException() instanceof CsvProcessingException))
        .andExpect(
            result ->
                assertEquals(
                    "Only CSV files are supported", result.getResolvedException().getMessage()));
  }
}