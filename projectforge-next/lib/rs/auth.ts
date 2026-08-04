// Authentication client: login, two-factor authentication, password reset.
//
// These endpoints are the next-only JSON counterparts of the UILayout based
// public pages (org.projectforge.rest.pub.next.*, org.projectforge.rest.my2fa.My2FANextRest).
// They return plain DTOs, so nothing here has to interpret a UILayout.

import { request } from "./client";
import type {
  WebAuthnFinishRequest,
  WebAuthnRequestOptions,
} from "../webauthn";

const LOGIN_PATH = "/rsPublic/nextLogin";
const RESET_PATH = "/rsPublic/nextPasswordReset";

/**
 * The three situations a second factor is asked for. Each has its own endpoint,
 * because the user is identified differently (pre-logged-in session, password
 * reset token, or fully logged-in session).
 */
export type TwoFactorContext = "login" | "reset" | "session";

const TWO_FACTOR_PATHS: Record<TwoFactorContext, string> = {
  login: "/rsPublic/next2FALogin",
  reset: RESET_PATH,
  session: "/rs/next2FA",
};

/** Mirrors org.projectforge.rest.pub.next.TwoFactorMethods. */
export interface TwoFactorMethods {
  otp: boolean;
  sms: boolean;
  mail: boolean;
  webAuthn: boolean;
  lastSuccessful2FA?: string | null;
}

export type LoginStatus = "SUCCESS" | "TWO_FACTOR_REQUIRED" | "FAILED";

export interface LoginResult {
  status: LoginStatus;
  messageKey?: string | null;
  /** Already localized by the server: on the login page the user isn't known yet. */
  message?: string | null;
  redirectUrl?: string | null;
  methods?: TwoFactorMethods | null;
}

export interface LoginState {
  loggedIn: boolean;
  twoFactorRequired: boolean;
  messageOfTheDay?: string | null;
  setupRedirectUrl?: string | null;
  methods?: TwoFactorMethods | null;
}

export interface TwoFactorResult {
  success: boolean;
  message?: string | null;
  redirectUrl?: string | null;
}

export interface TwoFactorState {
  methods: TwoFactorMethods;
  expiryMessage?: string | null;
}

export interface PasswordResetState {
  tokenValid: boolean;
  username?: string | null;
  twoFactorDone: boolean;
  methods?: TwoFactorMethods | null;
  csrfToken?: string | null;
}

/** Result with an optional field id, so a message can be attached to one input. */
export interface ActionResult {
  success: boolean;
  message?: string | null;
  field?: string | null;
}

// --- Login ---

/**
 * @param url Where to return to after the login (stored in the user's session).
 */
export function fetchLoginState(
  url?: string,
  signal?: AbortSignal
): Promise<LoginState> {
  const query = url ? `?url=${encodeURIComponent(url)}` : "";
  return request<LoginState>(
    `${LOGIN_PATH}/status${query}`,
    { method: "GET" },
    signal
  );
}

export function login(
  username: string,
  password: string,
  stayLoggedIn: boolean,
  signal?: AbortSignal
): Promise<LoginResult> {
  return request<LoginResult>(
    LOGIN_PATH,
    {
      method: "POST",
      body: JSON.stringify({ username, password, stayLoggedIn }),
    },
    signal
  );
}

/** Cancels a pending second factor (and clears the stay-logged-in cookie). */
export function cancelLogin(signal?: AbortSignal): Promise<ActionResult> {
  return request<ActionResult>(
    `${LOGIN_PATH}/cancel`,
    { method: "GET" },
    signal
  );
}

// --- Two-factor authentication (all three contexts) ---

export function fetchTwoFactorState(
  expiryMillis?: number,
  signal?: AbortSignal
): Promise<TwoFactorState> {
  const query = expiryMillis ? `?expiryMillis=${expiryMillis}` : "";
  return request<TwoFactorState>(
    `${TWO_FACTOR_PATHS.session}/status${query}`,
    { method: "GET" },
    signal
  );
}

export function checkOtp(
  context: TwoFactorContext,
  code: string,
  password?: string,
  signal?: AbortSignal
): Promise<TwoFactorResult> {
  return request<TwoFactorResult>(
    `${TWO_FACTOR_PATHS[context]}/checkOTP`,
    { method: "POST", body: JSON.stringify({ code, password }) },
    signal
  );
}

export function sendSmsCode(
  context: TwoFactorContext,
  signal?: AbortSignal
): Promise<TwoFactorResult> {
  return request<TwoFactorResult>(
    `${TWO_FACTOR_PATHS[context]}/sendSmsCode`,
    { method: "GET" },
    signal
  );
}

/** Not available for the password reset: the link was sent to that very mail account. */
export function sendMailCode(
  context: Exclude<TwoFactorContext, "reset">,
  signal?: AbortSignal
): Promise<TwoFactorResult> {
  return request<TwoFactorResult>(
    `${TWO_FACTOR_PATHS[context]}/sendMailCode`,
    { method: "GET" },
    signal
  );
}

/** Step 1 of WebAuthn: the challenge for navigator.credentials.get(). */
export function fetchWebAuthnOptions(
  context: TwoFactorContext,
  signal?: AbortSignal
): Promise<WebAuthnRequestOptions> {
  return request<WebAuthnRequestOptions>(
    `${TWO_FACTOR_PATHS[context]}/webAuthn`,
    { method: "GET" },
    signal
  );
}

/** Step 2 of WebAuthn: the signed response of the authenticator. */
export function finishWebAuthn(
  context: TwoFactorContext,
  webAuthnFinishRequest: WebAuthnFinishRequest,
  signal?: AbortSignal
): Promise<TwoFactorResult> {
  return request<TwoFactorResult>(
    `${TWO_FACTOR_PATHS[context]}/webAuthnFinish`,
    { method: "POST", body: JSON.stringify({ webAuthnFinishRequest }) },
    signal
  );
}

// --- Password forgotten / reset ---

export function requestPasswordResetMail(
  usernameEmail: string,
  signal?: AbortSignal
): Promise<ActionResult> {
  return request<ActionResult>(
    `${RESET_PATH}/requestMail`,
    { method: "POST", body: JSON.stringify({ usernameEmail }) },
    signal
  );
}

export function fetchPasswordResetState(
  token: string,
  signal?: AbortSignal
): Promise<PasswordResetState> {
  return request<PasswordResetState>(
    `${RESET_PATH}/status?token=${encodeURIComponent(token)}`,
    { method: "GET" },
    signal
  );
}

export function setNewPassword(
  newPassword: string,
  newPasswordRepeat: string,
  csrfToken: string,
  signal?: AbortSignal
): Promise<ActionResult> {
  return request<ActionResult>(
    RESET_PATH,
    {
      method: "POST",
      body: JSON.stringify({ newPassword, newPasswordRepeat, csrfToken }),
    },
    signal
  );
}

/** Invalidates the reset token (the link of the mail is unusable afterwards). */
export function cancelPasswordReset(
  signal?: AbortSignal
): Promise<ActionResult> {
  return request<ActionResult>(
    `${RESET_PATH}/cancel`,
    { method: "GET" },
    signal
  );
}
