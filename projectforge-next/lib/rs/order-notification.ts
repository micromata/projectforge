/**
 * The logged-in user's order notification settings (`MyAccountRest.orderNotificationPrefs`): a default mode and
 * exceptions for single projects or customers. Edited in a dialog of the order list and the order edit page.
 */

import { request } from "./client";

export type OrderNotificationMode = "IMMEDIATE" | "DAILY" | "MUTED";

/** Mirror of `MyAccountRest.OrderNotificationRule`: a project or customer with its own mode. */
export interface OrderNotificationRule {
  type: "PROJECT" | "CUSTOMER";
  id: number;
  /** For display only, ignored on saving. */
  displayName?: string | null;
  mode: OrderNotificationMode;
}

/** Mirror of `MyAccountRest.OrderNotificationSettings`. */
export interface OrderNotificationSettings {
  defaultMode: OrderNotificationMode;
  rules?: OrderNotificationRule[];
  writeAccess?: boolean;
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
