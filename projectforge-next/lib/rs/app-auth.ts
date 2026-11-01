/**
 * The app login (`AppAuthRest`, `/rs/appAuth`): the page `/appLogin` asks for the one-time code of the
 * ProjectForge app, which the browser then hands to the app via its callback url (see AppAuthService).
 */

import { request } from "./client";

/** The callback of the app (its url scheme); the server answers the url with the code itself. */
export const APP_CALLBACK_URL = "projectforge://auth";

/** Mirror of `AppAuthRest.AuthorizeRequest`; the values are those the app passed in the url. */
export interface AppAuthorizeRequest {
  challenge: string;
  state: string;
  deviceName?: string | null;
  platform?: string | null;
  /** The app is already registered and logs in again (new second factor). */
  deviceId?: number | null;
}

/** Mirror of `AppAuthRest.AuthorizeResponse`. */
export interface AppAuthorizeResponse {
  redirectUrl: string;
}

/**
 * Issues the one-time code. Requires a fresh second factor: the server's 403 opens the 2FA dialog and the
 * call is repeated (see `rawRequest`). HTTP 400 for a challenge or state the app didn't create.
 */
export function authorizeApp(
  body: AppAuthorizeRequest
): Promise<AppAuthorizeResponse> {
  return request<AppAuthorizeResponse>("/rs/appAuth/authorize", {
    method: "POST",
    body: JSON.stringify(body),
  });
}

/** Tells the app that the user declined: it closes the browser and shows its login again. */
export function appCancelUrl(state: string): string {
  return `${APP_CALLBACK_URL}?error=access_denied&state=${encodeURIComponent(state)}`;
}
