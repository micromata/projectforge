/**
 * The birthday butler (`org.projectforge.rest.birthdaybutler.BirthdayButlerRest`, page `/next/birthdayButler`):
 * the Word document with the birthdays of the organization's staff in a month. ORGA team and HR group only.
 *
 * A list that can't be created (no organization configured, no birthdays in the month, …) is an HTTP 400 whose
 * body is the backend's translated text, thrown by the download as an [RsError] carrying exactly that.
 */

import { request } from "./client";
import { downloadPost } from "./download";

export const BIRTHDAY_BUTLER_QUERY_KEY = ["birthdayButler"] as const;

/** `BirthdayButlerRest.Init`; `month` is 1-based (1 = January). */
export interface BirthdayButlerInit {
  month: number;
  configured: boolean;
}

export function fetchBirthdayButlerInit(
  signal?: AbortSignal
): Promise<BirthdayButlerInit> {
  return request<BirthdayButlerInit>(
    "/rs/birthdayButler/init",
    { method: "GET" },
    signal
  );
}

/** Saves the Word document of `month` (1-based) under the name the backend gave it. */
export function downloadBirthdayList(month: number): Promise<void> {
  return downloadPost("/rs/birthdayButler/download", { month });
}
