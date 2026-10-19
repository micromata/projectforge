/**
 * The 2FA setup page (`My2FASetupRest`, `/rs/2FASetup`): mobile phone, authenticator app and WebAuthn keys of
 * the logged-in user. The page itself is not behind a 2FA shortcut, but the key and every change need a recent
 * 2FA: the backend answers 403 with `twoFactorRequired`, which `rawRequest` handles by asking for the second
 * factor and repeating the call.
 */

import { request } from "./client";
import { postEntityAction, type EntityWriteResult } from "./entity";
import type {
  My2FAAuthenticatorKey,
  My2FASetupInit,
} from "@/components/features/my-2fa-setup/types";
import type {
  WebAuthnRegisterFinishRequest,
  WebAuthnRequestOptions,
} from "@/lib/webauthn";

export const MY_2FA_SETUP_QUERY_KEY = ["my2FASetup"] as const;

const ENTITY = "2FASetup";

export function fetch2FASetup(signal?: AbortSignal): Promise<My2FASetupInit> {
  return request<My2FASetupInit>(`/rs/${ENTITY}`, { method: "GET" }, signal);
}

/** The key of the authenticator app and its QR code. */
export function fetchAuthenticatorKey(
  signal?: AbortSignal
): Promise<My2FAAuthenticatorKey> {
  return request<My2FAAuthenticatorKey>(
    `/rs/${ENTITY}/authenticatorKey`,
    { method: "GET" },
    signal
  );
}

export function enableAuthenticatorApp(): Promise<EntityWriteResult> {
  return postEntityAction(ENTITY, "enableAuthenticatorApp", {});
}

export function disableAuthenticatorApp(): Promise<EntityWriteResult> {
  return postEntityAction(ENTITY, "disableAuthenticatorApp", {});
}

/** HTTP 406 carries the error of a malformed number (field `mobilePhone`). */
export function saveMobilePhone(
  mobilePhone: string
): Promise<EntityWriteResult> {
  return postEntityAction(ENTITY, "saveMobilePhone", { mobilePhone });
}

/** The challenge of a registration (`WebAuthnServicesRest.register`), with the user's keys to exclude. */
export function fetchWebAuthnRegisterOptions(
  signal?: AbortSignal
): Promise<WebAuthnRequestOptions> {
  return request<WebAuthnRequestOptions>(
    "/rs/webauthn/register",
    { method: "GET" },
    signal
  );
}

/** HTTP 406 carries the message of a failed registration. */
export function finishWebAuthnRegister(
  displayName: string,
  webAuthnFinishRequest: WebAuthnRegisterFinishRequest
): Promise<EntityWriteResult> {
  return postEntityAction(ENTITY, "webAuthnRegisterFinish", {
    displayName,
    webAuthnFinishRequest,
  });
}

export function updateWebAuthnEntry(
  id: number,
  displayName: string
): Promise<EntityWriteResult> {
  return postEntityAction(ENTITY, "webAuthnUpdate", { id, displayName });
}

export function deleteWebAuthnEntry(id: number): Promise<EntityWriteResult> {
  return postEntityAction(ENTITY, "webAuthnDelete", { id });
}
