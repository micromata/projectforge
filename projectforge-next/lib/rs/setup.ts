// Client for the first-run setup REST endpoint (/rsPublic/setup/*).
// All calls go through request() from lib/rs/client.ts, same as auth.ts.

import { request } from "./client";

const SETUP_PATH = "/rsPublic/setup";

/** A single timezone entry returned by the status endpoint. */
export interface TimeZoneInfo {
  id: string;
  displayName: string;
}

/** Response for GET /rsPublic/setup/status. */
export interface SetupState {
  alreadyInitialized: boolean;
  defaultUsername: string;
  defaultCalendarDomain: string;
  defaultTimeZone: string;
  availableTimeZones: TimeZoneInfo[];
}

/** Request body for POST /rsPublic/setup. */
export interface SetupPayload {
  setupTarget: "EMPTY_DATABASE" | "TEST_DATA";
  username: string;
  password: string;
  passwordRepeat: string;
  timeZone: string;
  calendarDomain: string;
  sysopEMail?: string;
  feedbackEMail?: string;
}

/** Response for POST /rsPublic/setup. */
export interface SetupResult {
  success: boolean;
  /** Localised error message when success is false. */
  message?: string | null;
  /** Form field the error belongs to, if applicable. */
  field?: string | null;
  /** On success: the URL the client should navigate to. */
  redirectUrl?: string | null;
}

export function fetchSetupState(signal?: AbortSignal): Promise<SetupState> {
  return request<SetupState>(`${SETUP_PATH}/status`, { method: "GET" }, signal);
}

export function submitSetup(
  payload: SetupPayload,
  signal?: AbortSignal
): Promise<SetupResult> {
  return request<SetupResult>(
    SETUP_PATH,
    { method: "POST", body: JSON.stringify(payload) },
    signal
  );
}
