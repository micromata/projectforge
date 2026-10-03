/**
 * The IHK training report of the IHK plugin (`org.projectforge.plugins.ihk.IHKRest`): the successor of the
 * plugin's Wicket page. Everything works on the logged-in user's own data.
 */

import { request } from "./client";
import { downloadPost } from "./download";
import type {
  IhkInit,
  IhkMissingDescription,
  IhkSettings,
} from "@/components/features/ihk/types";

/** The user's training settings (if set up) and the log viewer link. */
export function fetchIhkInit(signal?: AbortSignal): Promise<IhkInit> {
  return request<IhkInit>("/rs/ihk/init", { method: "GET" }, signal);
}

/**
 * Saves the user's training settings and answers them as stored. Invalid values answer 406, surfaced as an
 * RsError by `request`.
 */
export function saveIhkSettings(
  settings: IhkSettings,
  signal?: AbortSignal
): Promise<IhkSettings> {
  return request<IhkSettings>(
    "/rs/ihk/settings",
    { method: "POST", body: JSON.stringify(settings) },
    signal
  );
}

/** The time sheets without description of the week (Monday to Sunday) starting with `monday`. */
export function fetchIhkMissingDescriptions(
  monday: string,
  signal?: AbortSignal
): Promise<IhkMissingDescription[]> {
  return request<IhkMissingDescription[]>(
    `/rs/ihk/missingDescriptions?monday=${encodeURIComponent(monday)}`,
    { method: "GET" },
    signal
  );
}

/**
 * Downloads the training report (xlsx) of the week starting with `monday`. A missing setup or an empty week
 * throws an RsError carrying the server's message.
 */
export function downloadIhkReport(monday: string): Promise<void> {
  return downloadPost("/rs/ihk/export", { monday });
}
