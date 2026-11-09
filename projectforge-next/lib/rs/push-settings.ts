/**
 * The logged-in user's settings of the pushes to their apps (`PushSettingsRest`): whether a push shows the title and
 * the beginning of the notification. The same setting as in the app's settings.
 */

import { request } from "./client";

/** Mirror of `PushSettingsRest.Data`. */
export interface PushSettings {
  /** False if the server keeps the content out of every push: nothing to choose then. */
  available: boolean;
  content: boolean;
  /** False for a restricted or demo user (a shared account). */
  writeAccess?: boolean;
}

export const PUSH_SETTINGS_QUERY_KEY = ["myAccount", "pushSettings"] as const;

export function fetchPushSettings(signal?: AbortSignal): Promise<PushSettings> {
  return request<PushSettings>("/rs/pushSettings", { method: "GET" }, signal);
}

/** Saves the setting. Answers the settings as saved. */
export function savePushSettings(content: boolean): Promise<PushSettings> {
  return request<PushSettings>("/rs/pushSettings", {
    method: "POST",
    body: JSON.stringify({ content }),
  });
}
