import * as yup from "yup";
import { PhoneNumberUtil } from "google-libphonenumber";

import {
  RACE_VALUES,
  ROLE_VALUES,
  ETHNICITY_VALUES,
  GENDER_VALUES,
  TRIBAL_AFFILIATION_VALUES,
} from "../constants";
import { Option } from "../commonComponents/Dropdown";
import { languages } from "../../config/constants";

const phoneUtil = PhoneNumberUtil.getInstance();

type PartialBy<T, K extends keyof T> = Omit<T, K> & Partial<Pick<T, K>>;

type UpdateOptionalFields =
  | "lookupId"
  | "role"
  | "email"
  | "streetTwo"
  | "city"
  | "county"
  | "race"
  | "ethnicity"
  | "gender"
  | "tribalAffiliation"
  | "residentCongregateSetting"
  | "employedInHealthcare"
  | "preferredLanguage";

type OptionalFields = UpdateOptionalFields | "middleName";

export type RequiredPersonFields = PartialBy<
  Nullable<PersonFormData>,
  OptionalFields
>;

export type PersonUpdateFields = PartialBy<
  Nullable<PersonUpdate>,
  UpdateOptionalFields
>;

const getValues = (options: Option[]) => options.map(({ value }) => value);

const updateFieldSchemata: Record<keyof PersonUpdate, yup.AnySchema> = {
  lookupId: yup.string().nullable(),
  role: yup.mixed().oneOf([...getValues(ROLE_VALUES), "UNKNOWN", "", null]),
  telephone: yup
    .string()
    .test(function (input) {
      if (!input) {
        return false;
      }
      const number = phoneUtil.parseAndKeepRawInput(input, "US");
      return phoneUtil.isValidNumber(number);
    })
    .required(),
  email: yup.string().email().nullable(),
  street: yup.string().required(),
  streetTwo: yup.string().nullable(),
  city: yup.string().nullable(),
  county: yup.string().nullable(),
  state: yup.string().required(),
  zipCode: yup.string().required(),
  race: yup.mixed().oneOf([...getValues(RACE_VALUES), "", null]),
  ethnicity: yup.mixed().oneOf([...getValues(ETHNICITY_VALUES), "", null]),
  gender: yup.mixed().oneOf([...getValues(GENDER_VALUES), "", null]),
  residentCongregateSetting: yup.boolean().nullable(),
  employedInHealthcare: yup.boolean().nullable(),
  tribalAffiliation: yup
    .mixed()
    .oneOf([...getValues(TRIBAL_AFFILIATION_VALUES), "", null]),
  preferredLanguage: yup.mixed().oneOf([...languages, "", null]),
};

export const personUpdateSchema: yup.SchemaOf<PersonUpdateFields> = yup.object(
  updateFieldSchemata
);

export const personSchema: yup.SchemaOf<RequiredPersonFields> = yup.object({
  firstName: yup.string().required(),
  middleName: yup.string().nullable(),
  lastName: yup.string().required(),
  birthDate: yup.string().required(),
  facilityId: yup.string().nullable().min(1) as any,
  ...updateFieldSchemata,
});

export type PersonErrors = Partial<Record<keyof PersonFormData, string>>;

export const allPersonErrors: Required<PersonErrors> = {
  firstName: "First name is required",
  middleName: "Middle name is incorrectly formatted",
  lastName: "Last name is required",
  lookupId: "Lookup ID is incorrectly formatted",
  role: "Role is incorrectly formatted",
  facilityId: "Facility is required",
  birthDate: "Date of birth is missing or incorrectly formatted",
  telephone: "Phone number is missing or invalid",
  email: "Email is missing or incorrectly formatted",
  street: "Street is missing",
  streetTwo: "Street Two is incorrectly formatted",
  zipCode: "Zip code is missing or incorrectly formatted",
  state: "State is missing or incorrectly formatted",
  city: "City is incorrectly formatted",
  county: "County is incorrectly formatted",
  race: "Race is incorrectly formatted",
  tribalAffiliation: "Tribal Affiliation is incorrectly formatted",
  ethnicity: "Ethnicity is incorrectly formatted",
  gender: "Biological Sex is incorrectly formatted",
  residentCongregateSetting:
    "Are you a resident in a congregate living setting? is required",
  employedInHealthcare: "Are you a health care worker? is required",
  preferredLanguage: "Preferred language is incorrectly formatted",
};