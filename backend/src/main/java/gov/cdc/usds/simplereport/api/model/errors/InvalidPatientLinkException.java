package gov.cdc.usds.simplereport.api.model.errors;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(
    code = HttpStatus.FORBIDDEN,
    reason =
        "No patient link with the supplied ID was found, or the birth date provided was incorrect.")
public class InvalidPatientLinkException extends RuntimeException {
  private static final long serialVersionUID = 1L;
}