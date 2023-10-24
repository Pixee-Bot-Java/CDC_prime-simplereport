import React from "react";

import {
  MULTIPLEX_DISEASES,
  TEST_RESULTS,
} from "../../../testResults/constants";
import { findResultByDiseaseName } from "../../QueueItem";
import { MultiplexResultInput } from "../../../../generated/graphql";
import RadioGroup from "../../../commonComponents/RadioGroup";
import { COVID_RESULTS, TEST_RESULT_DESCRIPTIONS } from "../../../constants";

interface CovidResult extends MultiplexResultInput {
  diseaseName: MULTIPLEX_DISEASES.COVID_19;
  testResult: TestResult;
}

const convertFromMultiplexResultInputs = (
  multiplexResultInputs: MultiplexResultInput[]
): TestResult => {
  return (
    (findResultByDiseaseName(
      multiplexResultInputs ?? [],
      MULTIPLEX_DISEASES.COVID_19
    ) as TestResult) ?? TEST_RESULTS.UNKNOWN
  );
};

const convertFromCovidResult = (covidResult: TestResult): CovidResult[] => {
  const covidResults: CovidResult[] = [
    {
      diseaseName: MULTIPLEX_DISEASES.COVID_19,
      testResult: covidResult,
    },
  ];

  return covidResults.filter(
    (result) => result.testResult !== TEST_RESULTS.UNKNOWN
  );
};

export const validateCovidResultInput = (
  testResults: MultiplexResultInput[]
) => {
  const resultCovidFormat = convertFromMultiplexResultInputs(testResults);
  if (resultCovidFormat === TEST_RESULTS.UNKNOWN) {
    return "Please enter a COVID-19 test result.";
  }
  return "";
};

interface Props {
  queueItemId: string;
  testResults: MultiplexResultInput[];
  onChange: (value: CovidResult[]) => void;
}

const CovidResultInputGroup: React.FC<Props> = ({
  queueItemId,
  testResults,
  onChange,
}) => {
  const resultCovidFormat = convertFromMultiplexResultInputs(testResults);

  const convertAndSendResults = (covidResult: TestResult) => {
    const results = convertFromCovidResult(covidResult);
    onChange(results);
  };

  return (
    <RadioGroup
      legend="COVID-19 result"
      onChange={(value: TestResult) => {
        convertAndSendResults(value);
      }}
      buttons={[
        {
          value: COVID_RESULTS.POSITIVE,
          label: `${TEST_RESULT_DESCRIPTIONS.POSITIVE} (+)`,
        },
        {
          value: COVID_RESULTS.NEGATIVE,
          label: `${TEST_RESULT_DESCRIPTIONS.NEGATIVE} (-)`,
        },
        {
          value: COVID_RESULTS.INCONCLUSIVE,
          label: `${TEST_RESULT_DESCRIPTIONS.UNDETERMINED}`,
        },
      ]}
      name={`covid-test-result-${queueItemId}`}
      selectedRadio={resultCovidFormat}
      wrapperClassName="prime-radio__group"
      required
    />
  );
};

export default CovidResultInputGroup;