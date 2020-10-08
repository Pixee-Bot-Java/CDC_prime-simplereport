import axios from "axios";
import { ROOT_URL } from "../../config/constants";
import { samplePatients } from "../test/data/patients";
import { COVID_RESULTS } from "../constants";
import { isLocalHost } from "../utils/";
import { mapApiDataToClient } from "../utils/mappers";
import { testResultMapping } from "./mappings";

export const getTestResult = async (patientId) => {
  if (isLocalHost) {
    const url = `${ROOT_URL}/test_results/${patientId}`;
    const response = await axios.get(url);
    const rawTestResult = response.data;
    const testResult = mapApiDataToClient(rawTestResult, testResultMapping);
    return testResult;
  }
  return {
    testResult: COVID_RESULTS.DETECTED,
    ...samplePatients.find(
      (samplepatient) => samplepatient.patientId === patientId
    ),
  };
};
