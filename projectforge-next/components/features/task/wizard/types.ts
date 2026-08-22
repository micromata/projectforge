import type { EntityRef } from "@/components/shared/entity-autocomplete";
import type { TaskWizardRequest } from "@/lib/rs/task";

/**
 * The three roles a group can be given, named as `TaskWizardService.GroupType.i18nKey`: the key is the
 * suffix under `task.wizard.` that holds the heading and the intro of the step, so one component
 * serves all three.
 */
export type WizardGroupKey = "team" | "managerGroup" | "externalGroup";

export interface WizardGroupStep {
  key: WizardGroupKey;
  /** Field of the request the picked group goes into. */
  field: keyof Omit<TaskWizardRequest, "taskId">;
  /**
   * Whether the bundle holds a `groupNameSuffix` for this role — the name Wicket prefilled the new
   * group's form with, `<task title>-<suffix>`. The team has none: there the title alone is the name.
   */
  hasNameSuffix: boolean;
}

/**
 * The steps 2 to 4, in the order Wicket numbers them (`TaskWizardForm.init`) — the team first, because
 * it is the group most structure elements get and the other two are the exception.
 */
export const GROUP_STEPS: WizardGroupStep[] = [
  { key: "team", field: "teamGroupId", hasNameSuffix: false },
  { key: "managerGroup", field: "managerGroupId", hasNameSuffix: true },
  { key: "externalGroup", field: "externalGroupId", hasNameSuffix: true },
];

/** The groups picked so far, one entry per role that has one. */
export type WizardGroups = Partial<Record<WizardGroupKey, EntityRef | null>>;
