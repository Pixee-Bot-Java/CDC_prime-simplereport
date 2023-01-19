import { MemoryRouter, Route, Routes } from "react-router-dom";
import { Provider } from "react-redux";
import createMockStore, { MockStoreEnhanced } from "redux-mock-store";
import { MockedProvider, MockedResponse } from "@apollo/client/testing";
import {
  render,
  screen,
  waitForElementToBeRemoved,
} from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { ApplicationInsights } from "@microsoft/applicationinsights-web";
import jwtDecode from "jwt-decode";
import MockDate from "mockdate";

import {
  GetFacilityQueueDocument,
  GetFacilityQueueQuery,
  GetTopLevelDashboardMetricsNewDocument,
} from "../generated/graphql";

import ReportingApp, { WHOAMI_QUERY } from "./ReportingApp";
import PrimeErrorBoundary from "./PrimeErrorBoundary";
import { TRAINING_PURPOSES_ONLY } from "./commonComponents/TrainingNotification";
import {
  getEndDateFromDaysAgo,
  getStartDateFromDaysAgo,
} from "./analytics/Analytics";
import { getAppInsights } from "./TelemetryService";

const mockDispatch = jest.fn();

jest.mock("uuid");
jest.mock("./VersionService", () => ({
  VersionService: {
    enforce: jest.fn(),
  },
}));
jest.mock("./testResults/CleanTestResultsList", () => {
  return () => <p>CleanTestResultsList</p>;
});
jest.mock("./testResults/TestResultsList", () => {
  return () => <p>TestResultsList</p>;
});
jest.mock("./TelemetryService", () => ({
  ...jest.requireActual("./TelemetryService"),
  getAppInsights: jest.fn(),
}));
jest.mock("jwt-decode", () => jest.fn());

const mockStore = createMockStore([]);

const store = {
  dispatch: mockDispatch,
  organization: {
    name: "Organization Name",
  },
  user: {
    id: "05b2f71a-9392-442b-aab5-4eb550a864c0",
    firstName: "Bob",
    middleName: null,
    lastName: "Bobberoo",
    suffix: null,
    email: "bob@example.com",
    roleDescription: "Admin user",
    isAdmin: false,
    permissions: [
      "EDIT_PATIENT",
      "ARCHIVE_PATIENT",
      "READ_PATIENT_LIST",
      "EDIT_ORGANIZATION",
      "START_TEST",
      "EDIT_FACILITY",
      "ACCESS_ALL_FACILITIES",
      "READ_RESULT_LIST",
      "READ_ARCHIVED_PATIENT_LIST",
      "SUBMIT_TEST",
      "MANAGE_USERS",
      "SEARCH_PATIENTS",
      "UPDATE_TEST",
    ],
  },
  facilities: [
    {
      id: "fec4de56-f4cc-4c61-b3d5-76869ca71296",
      name: "Testing Site",
    },
  ],
  facility: {
    id: "fec4de56-f4cc-4c61-b3d5-76869ca71296",
    name: "Testing Site",
  },
};

store.dispatch = mockDispatch;

const WhoAmIQueryMock = {
  request: {
    query: WHOAMI_QUERY,
    fetchPolicy: "no-cache",
  },
  result: {
    data: {
      whoami: {
        id: "05b2f71a-9392-442b-aab5-4eb550a864c0",
        firstName: "Bob",
        middleName: null,
        lastName: "Bobberoo",
        suffix: null,
        email: "bob@example.com",
        isAdmin: false,
        permissions: [
          "EDIT_PATIENT",
          "ARCHIVE_PATIENT",
          "READ_PATIENT_LIST",
          "EDIT_ORGANIZATION",
          "START_TEST",
          "EDIT_FACILITY",
          "ACCESS_ALL_FACILITIES",
          "READ_RESULT_LIST",
          "READ_ARCHIVED_PATIENT_LIST",
          "SUBMIT_TEST",
          "MANAGE_USERS",
          "SEARCH_PATIENTS",
          "UPDATE_TEST",
        ],
        roleDescription: "Admin user",
        organization: {
          name: "Dis Organization",
          testingFacility: [
            {
              id: "fec4de56-f4cc-4c61-b3d5-76869ca71296",
              name: "Testing Site",
            },
          ],
        },
      },
    },
  },
};
const facilityQueryMock = {
  request: {
    query: GetFacilityQueueDocument,
    fetchPolicy: "no-cache",
    variables: {
      facilityId: "fec4de56-f4cc-4c61-b3d5-76869ca71296",
    },
  },
  result: {
    data: {
      queue: [],
      facility: {
        id: "fec4de56-f4cc-4c61-b3d5-76869ca71296",
        name: "Testing site",
        deviceTypes: [
          {
            internalId: "d70bb3b8-96bd-40d9-a3ce-b266a7edb91d",
            name: "Quidel Sofia 2",
            testLength: 15,
            supportedDiseases: [],
            swabTypes: [],
          },
          {
            internalId: "5e44dcef-8cc6-44f4-a200-a5b8169ab60a",
            name: "LumiraDX",
            testLength: 15,
            supportedDiseases: [],
            swabTypes: [],
          },
        ],
      },
    } as GetFacilityQueueQuery,
  },
};
const WhoAmIErrorQueryMock = {
  request: {
    query: WHOAMI_QUERY,
    fetchPolicy: "no-cache",
  },
  error: new Error("Server connection error"),
};
const getAnalyticsQueryMock = () => ({
  request: {
    query: GetTopLevelDashboardMetricsNewDocument,
    variables: {
      facilityId: "",
      startDate: getStartDateFromDaysAgo(7),
      endDate: getEndDateFromDaysAgo(0),
    },
  },
  result: {
    data: {
      topLevelDashboardMetrics: {
        totalTestCount: 124820,
        positiveTestCount: 2270,
      },
    },
  },
});

const renderApp = (
  newStore: MockStoreEnhanced<unknown, {}>,
  queryMocks: MockedResponse[]
) => {
  return render(
    <PrimeErrorBoundary>
      <Provider store={newStore}>
        <MockedProvider mocks={queryMocks} addTypename={false}>
          <MemoryRouter>
            <Routes>
              <Route path="/*" element={<ReportingApp />} />
            </Routes>
          </MemoryRouter>
        </MockedProvider>
      </Provider>
    </PrimeErrorBoundary>
  );
};

const MODAL_TEXT = "Welcome to the SimpleReport";

describe("App", () => {
  beforeEach(() => {
    MockDate.set("2021-08-01");
  });

  afterEach(() => {
    MockDate.reset();
  });

  it("Render first loading screen", async () => {
    const mockedStore = mockStore({});
    renderApp(mockedStore, [WhoAmIQueryMock]);
    await screen.findByText("Loading account information...");
  });

  it("Render facility loading", async () => {
    const mockedStore = mockStore({ ...store });
    renderApp(mockedStore, [WhoAmIQueryMock, facilityQueryMock]);
    expect(await screen.findByText("Loading facility information..."));
  });

  it("Render main screen", async () => {
    const mockedStore = mockStore({ ...store, dataLoaded: true });
    renderApp(mockedStore, [
      WhoAmIQueryMock,
      facilityQueryMock,
      getAnalyticsQueryMock(),
    ]);
    await waitForElementToBeRemoved(() =>
      screen.queryByText("Loading account information...")
    );
    await userEvent.click(
      screen.getAllByText("Testing Site", { exact: false })[0]
    );
    expect(
      await screen.findByText("COVID-19 testing data")
    ).toBeInTheDocument();
  });

  it("should show error UI", async () => {
    const mockedStore = mockStore({ ...store, dataLoaded: true });
    const { container } = renderApp(mockedStore, [WhoAmIErrorQueryMock]);
    await screen.findByText("error", { exact: false });
    expect(container).toMatchSnapshot();
  });

  it("displays the training header and modal and dismisses the modal", async () => {
    process.env.REACT_APP_IS_TRAINING_SITE = "true";
    const mockedStore = mockStore({ ...store, dataLoaded: true });
    renderApp(mockedStore, [
      WhoAmIQueryMock,
      facilityQueryMock,
      getAnalyticsQueryMock(),
    ]);
    expect(await screen.findAllByText(TRAINING_PURPOSES_ONLY)).toHaveLength(2);
    const trainingWelcome = await screen.findByText(MODAL_TEXT, {
      exact: false,
    });
    expect(trainingWelcome).toBeInTheDocument();
    await userEvent.click(screen.getByText("Got it", { exact: false }));
    expect(trainingWelcome).not.toBeInTheDocument();
  });

  it("does not display training notifications outside the training environment", () => {
    process.env.REACT_APP_IS_TRAINING_SITE = "false";
    const mockedStore = mockStore({ ...store, dataLoaded: true });
    renderApp(mockedStore, [WhoAmIQueryMock, facilityQueryMock]);
    expect(screen.queryByText(TRAINING_PURPOSES_ONLY)).not.toBeInTheDocument();
    expect(screen.queryByText(MODAL_TEXT)).not.toBeInTheDocument();
  });

  describe("logs to App Insights on WhoAmI error", () => {
    const oldTokenClaim = process.env.REACT_APP_OKTA_TOKEN_ROLE_CLAIM;
    const trackExceptionMock = jest.fn();
    beforeEach(() => {
      process.env.REACT_APP_OKTA_TOKEN_ROLE_CLAIM = "test_roles";
      trackExceptionMock.mockReset();
      (getAppInsights as jest.Mock).mockImplementation(() => {
        const ai = Object.create(ApplicationInsights.prototype);
        return Object.assign(ai, { trackException: trackExceptionMock });
      });
    });
    afterEach(() => {
      process.env.REACT_APP_OKTA_TOKEN_ROLE_CLAIM = oldTokenClaim;
      jest.spyOn(Storage.prototype, "getItem").mockRestore();
    });
    it("logs with access token info", async () => {
      jest
        .spyOn(Storage.prototype, "getItem")
        .mockImplementation(() => "definitely a valid token");
      (jwtDecode as jest.Mock).mockReturnValue({
        sub: "subject@fakeorg.net",
        test_roles: [
          "SR-FAKE-TENANT:XX-TestOrg-123:NO_ACCESS",
          "SR-FAKE-TENANT:XX-TestOrg-123:USER",
        ],
      });
      const mockedStore = mockStore({ ...store, dataLoaded: true });

      renderApp(mockedStore, [WhoAmIErrorQueryMock]);
      await screen.findByText("error", { exact: false });

      expect(trackExceptionMock).toHaveBeenCalledWith({
        exception: new Error("Server connection error"),
        properties: {
          "user message": "Server connection error",
          "valid access token": true,
          "token subject": "subject@fakeorg.net",
          "token roles": [
            "SR-FAKE-TENANT:XX-TestOrg-123:NO_ACCESS",
            "SR-FAKE-TENANT:XX-TestOrg-123:USER",
          ],
        },
      });
    });
    it("still logs if missing token", async () => {
      const mockedStore = mockStore({ ...store, dataLoaded: true });
      jest.spyOn(Storage.prototype, "getItem").mockImplementation(() => null);

      renderApp(mockedStore, [WhoAmIErrorQueryMock]);
      await screen.findByText("error", { exact: false });

      expect(trackExceptionMock).toHaveBeenCalledWith({
        exception: new Error("Server connection error"),
        properties: {
          "user message": "Server connection error",
          "valid access token": null,
          "token subject": undefined,
          "token roles": undefined,
        },
      });
    });
    it("still logs if invalid token", async () => {
      jest
        .spyOn(Storage.prototype, "getItem")
        .mockImplementation(() => "definitely NOT a valid token");
      jest.spyOn(console, "error").mockImplementation(() => {});
      (jwtDecode as jest.Mock).mockImplementation(() => {
        throw new Error("InvalidToken");
      });
      const mockedStore = mockStore({ ...store, dataLoaded: true });

      renderApp(mockedStore, [WhoAmIErrorQueryMock]);
      await screen.findByText("error", { exact: false });

      expect(trackExceptionMock).toHaveBeenCalledWith({
        exception: new Error("Server connection error"),
        properties: {
          "user message": "Server connection error",
          "valid access token": false,
          "token subject": undefined,
          "token roles": undefined,
        },
      });
      jest.spyOn(console, "error").mockRestore();
    });
  });
  it("renders CleanTestResultsList when going to /results/", async () => {
    const mockedStore = mockStore({ ...store, dataLoaded: true });

    render(
      <Provider store={mockedStore}>
        <MockedProvider mocks={[WhoAmIQueryMock]} addTypename={false}>
          <MemoryRouter
            initialEntries={[
              `/results?facility=fec4de56-f4cc-4c61-b3d5-76869ca71296`,
            ]}
          >
            <ReportingApp />
          </MemoryRouter>
        </MockedProvider>
      </Provider>
    );

    expect(await screen.findByText("CleanTestResultsList")).toBeInTheDocument();
  });

  it("renders TestResultsList when going to /results/page_number ", async () => {
    const mockedStore = mockStore({ ...store, dataLoaded: true });

    render(
      <Provider store={mockedStore}>
        <MockedProvider mocks={[WhoAmIQueryMock]} addTypename={false}>
          <MemoryRouter
            initialEntries={[
              `/results/1?facility=fec4de56-f4cc-4c61-b3d5-76869ca71296`,
            ]}
          >
            <ReportingApp />
          </MemoryRouter>
        </MockedProvider>
      </Provider>
    );
    expect(await screen.findByText("TestResultsList")).toBeInTheDocument();
  });
});