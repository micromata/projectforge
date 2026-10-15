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

/**
 * The logged-in user's employee as an entity reference, or null for an account without one. An
 * employee's display name is its user's full name, so the name is taken from the user.
 */
export function useCurrentEmployeeRef(): EntityRef | null {
  const { user } = useAuth();
  if (user?.employeeId == null) return null;
  return { id: user.employeeId, displayName: user.fullname || user.username };
}

/**
 * The „select me" entry of a picker searching [entity] (`user`, `employee`, a REST category): the
 * logged-in user or their employee, and null for any other entity — picking oneself means nothing for a
 * project or a cost unit. The one rule every person picker follows (see [PersonSelect]).
 */
export function useSelectMeRef(entity: string | undefined): EntityRef | null {
  const user = useCurrentUserRef();
  const employee = useCurrentEmployeeRef();
  if (entity === "user") return user;
  if (entity === "employee") return employee;
  return null;
}
