"use client";

import type { EntityRef } from "@/components/shared/entity-autocomplete";
import { useAuth } from "./use-auth";

/**
 * The logged-in user as an entity reference, the shape a picker and a DTO field carry
 * (`{id, displayName}`).
 *
 * `displayName` is `PFUserDO.displayName`, which is the full name — the username only stands in where
 * the account has none, as `getFullname()` itself does. Only the id is written: `BaseDTO.copyTo`
 * resolves the reference by it, so the name is display alone.
 */
export function useCurrentUserRef(): EntityRef | null {
  const { user } = useAuth();
  if (!user) return null;
  return { id: user.userId, displayName: user.fullname || user.username };
}
