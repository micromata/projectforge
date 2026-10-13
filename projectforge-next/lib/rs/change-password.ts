/**
 * The change password pages (`ChangePasswordPageRest`, `/rs/changePassword`, and `ChangeWlanPasswordPageRest`,
 * `/rs/changeWlanPassword`). All calls are behind the 2FA shortcut PASSWORD, which `rawRequest` answers with
 * its own dialog. Another user's password (`userId`) may be changed by admins only.
 */

import { request } from "./client";
import { postEntityAction, type EntityWriteResult } from "./entity";
import type {
  ChangePasswordInit,
  ChangePasswordValues,
  PasswordVariant,
} from "@/components/features/change-password/types";

function restPath(variant: PasswordVariant): string {
  return variant === "wlan" ? "changeWlanPassword" : "changePassword";
}

export function changePasswordQueryKey(
  variant: PasswordVariant,
  userId: string | null
) {
  return ["changePassword", variant, userId ?? "own"] as const;
}

/** The user, the date of the last change and the password rules. */
export function fetchChangePasswordInit(
  variant: PasswordVariant,
  userId: string | null,
  signal?: AbortSignal
): Promise<ChangePasswordInit> {
  const query = userId ? `?userId=${encodeURIComponent(userId)}` : "";
  return request<ChangePasswordInit>(
    `/rs/${restPath(variant)}/init${query}`,
    { method: "GET" },
    signal
  );
}

/** Changes the password; HTTP 406 carries the `validationErrors`, mostly with the field they belong to. */
export function changePassword(
  variant: PasswordVariant,
  values: ChangePasswordValues
): Promise<EntityWriteResult> {
  return postEntityAction(restPath(variant), "change", values);
}
