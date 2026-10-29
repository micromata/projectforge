/**
 * The my-account page (`MyAccountRest`, `/rs/myAccount`): the logged-in user's own data, settings, access
 * tokens and stay-logged-in devices. All calls are behind the 2FA shortcut MY_ACCOUNT, which `rawRequest`
 * handles by asking for the second factor and repeating the call.
 */

import { request } from "./client";
import { postEntityAction, type EntityWriteResult } from "./entity";
import type {
  MyAccountDevice,
  MyAccountInit,
  MyAccountToken,
  MyAccountTokenInfo,
  MyAccountTokenType,
  OrderNotificationSettings,
} from "@/components/features/my-account/types";
import type { MyAccountValues } from "@/components/features/my-account/schema";

export const MY_ACCOUNT_QUERY_KEY = ["myAccount"] as const;

/** The user's data, the choices of the settings and the links of the password pages. */
export function fetchMyAccount(signal?: AbortSignal): Promise<MyAccountInit> {
  return request<MyAccountInit>("/rs/myAccount", { method: "GET" }, signal);
}

/** Saves the editable fields; HTTP 406 carries the `validationErrors` (see lib/rs/entity.ts). */
export function saveMyAccount(
  values: MyAccountValues
): Promise<EntityWriteResult> {
  return postEntityAction("myAccount", "save", values);
}

/** Renews an access token: the old one is invalid from now on. Answers the new one. */
export function renewMyAccountToken(
  type: MyAccountTokenType
): Promise<MyAccountToken> {
  return request<MyAccountToken>(
    `/rs/myAccount/renewToken?token=${encodeURIComponent(type)}`,
    { method: "POST" }
  );
}

/** The accesses with a token since the server start and, for the REST client token, its QR code. */
export function fetchMyAccountTokenInfo(
  type: MyAccountTokenType,
  signal?: AbortSignal
): Promise<MyAccountTokenInfo> {
  return request<MyAccountTokenInfo>(
    `/rs/myAccount/tokenInfo?token=${encodeURIComponent(type)}`,
    { method: "GET" },
    signal
  );
}

/** Logs out one stay-logged-in device of the user. Answers the remaining devices. */
export function logoutMyAccountDevice(id: number): Promise<MyAccountDevice[]> {
  return request<MyAccountDevice[]>(
    `/rs/myAccount/logoutDevice?id=${encodeURIComponent(id)}`,
    { method: "POST" }
  );
}

/** Logs out all stay-logged-in devices of the user, this one included. Answers the remaining (none). */
export function logoutAllMyAccountDevices(): Promise<MyAccountDevice[]> {
  return request<MyAccountDevice[]>("/rs/myAccount/logoutAllDevices", {
    method: "POST",
  });
}

export const ORDER_NOTIFICATION_QUERY_KEY = [
  "myAccount",
  "orderNotificationPrefs",
] as const;

/** The user's order notification settings: default mode and the exceptions for projects and customers. */
export function fetchOrderNotificationSettings(
  signal?: AbortSignal
): Promise<OrderNotificationSettings> {
  return request<OrderNotificationSettings>(
    "/rs/myAccount/orderNotificationPrefs",
    { method: "GET" },
    signal
  );
}

/** Saves the order notification settings. Answers them as saved. */
export function saveOrderNotificationSettings(
  settings: OrderNotificationSettings
): Promise<OrderNotificationSettings> {
  return request<OrderNotificationSettings>(
    "/rs/myAccount/orderNotificationPrefs",
    { method: "POST", body: JSON.stringify(settings) }
  );
}
