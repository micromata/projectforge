import type { EntityRef } from "@/components/shared/entity-autocomplete";
import type { WizardGroupKey } from "@/components/features/task/wizard/types";

/** How a role gets its group: none at all, an existing one, or a new one the wizard creates. */
export type RoleMode = "none" | "existing" | "new";

/**
 * What one role step holds.
 *
 * @param name The name of a new group as typed, null as long as the user typed none — the suggestion
 *   made of the project's name stands in then, and follows the name as it is typed.
 */
export interface RoleState {
  mode: RoleMode;
  group: EntityRef | null;
  name: string | null;
  members: EntityRef[];
}

export type RoleStates = Record<WizardGroupKey, RoleState>;

/**
 * The roles a new project starts with: no group at all, so nothing is granted unless a group is picked
 * or a new one entered.
 */
export function initialRoles(): RoleStates {
  const role = (mode: RoleMode): RoleState => ({
    mode,
    group: null,
    name: null,
    members: [],
  });
  return {
    team: role("none"),
    managerGroup: role("none"),
    externalGroup: role("none"),
  };
}

/** The field of `ProjectWizardService.Data` a role goes into, the prefix of its field errors. */
export const ROLE_FIELDS: Record<
  WizardGroupKey,
  "teamGroup" | "managerGroup" | "externalGroup"
> = {
  team: "teamGroup",
  managerGroup: "managerGroup",
  externalGroup: "externalGroup",
};

/** How the wizard gets a structure element of the customer or the project: a new one, or an existing one. */
export type ElementMode = "new" | "existing";
