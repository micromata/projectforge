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
 * The roles a new project starts with: a new team and a new group of managers, which most projects
 * have, but no externals, which few have.
 */
export function initialRoles(): RoleStates {
  const role = (mode: RoleMode): RoleState => ({
    mode,
    group: null,
    name: null,
    members: [],
  });
  return {
    team: role("new"),
    managerGroup: role("new"),
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
