// Client for the first-run setup REST endpoint (/rsPublic/setup/*).
// All calls go through request() from lib/rs/client.ts, same as auth.ts.

import { request } from "./client";

const SETUP_PATH = "/rsPublic/setup";

/** A single timezone entry returned by the status endpoint. */
export interface TimeZoneInfo {
  id: string;
  displayName: string;
}

/** Regional settings of projectforge.properties; blank or missing = unchanged. */
export interface RegionalSettings {
  /** ISO 4217 code, e.g. EUR. */
  currency?: string | null;
  /** Symbol behind amounts, e.g. €. */
  currencySymbol?: string | null;
  /** One of SetupState.availableLocales. */
  defaultLocale?: string | null;
  /** One of SetupState.availableTimeNotations (H12, H24). */
  defaultTimeNotation?: string | null;
  /** One of SetupState.availableFirstDaysOfWeek (MONDAY, ...). */
  defaultFirstDayOfWeek?: string | null;
}

/** Response for GET /rsPublic/setup/status. */
export interface SetupState {
  alreadyInitialized: boolean;
  defaultUsername: string;
  defaultCalendarDomain: string;
  defaultTimeZone: string;
  availableTimeZones: TimeZoneInfo[];
  /** The regional settings of the running system (preset of the form). */
  regional: RegionalSettings;
  availableLocales: string[];
  availableTimeNotations: string[];
  availableFirstDaysOfWeek: string[];
  /** ISO 4217 codes for the currency picker. */
  availableCurrencies: string[];
}

/** Response for GET /rsPublic/setup/progress. */
export interface SetupProgress {
  /** True while a setup is running. */
  running: boolean;
  /** Progress in percent, null if no setup is running. */
  percent?: number | null;
}

/** Request body for POST /rsPublic/setup. */
export interface SetupPayload extends RegionalSettings {
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

export function fetchSetupProgress(
  signal?: AbortSignal
): Promise<SetupProgress> {
  return request<SetupProgress>(
    `${SETUP_PATH}/progress`,
    { method: "GET" },
    signal
  );
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
