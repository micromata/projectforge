import type {
  TaskWizardAccessEntry,
  TaskWizardAccessStatus,
  TaskWizardGroupType,
} from "@/lib/rs/task";

/** The badge text of a status, kept as literals so the i18n key scanner sees them. */
export const STATUS_KEYS: Record<TaskWizardAccessStatus, string> = {
  CREATED: "task.wizard.result.created",
  UPDATED: "task.wizard.result.updated",
  UNCHANGED: "task.wizard.result.unchanged",
};

/**
 * The role a group was granted. Not `task.wizard.<key>`, which the steps use: those texts are the
 * form labels of the steps and read „Managing users (optional)".
 */
const ROLE_KEYS: Record<TaskWizardGroupType, string> = {
  MANAGER: "task.wizard.result.role.manager",
  TEAM: "task.wizard.result.role.team",
  EXTERNAL: "task.wizard.result.role.external",
};

/** The order the statuses are counted in, most notable first. */
const STATUS_ORDER: TaskWizardAccessStatus[] = [
  "CREATED",
  "UPDATED",
  "UNCHANGED",
];

/** What one group got: the right on the picked element, and the read access on its ancestors. */
export interface WizardResultBlock {
  groupType: TaskWizardGroupType;
  groupName?: string | null;
  roleKey: string;
  /** The entry of the element the user picked; absent for the root, which never gets one. */
  picked?: TaskWizardAccessEntry;
  ancestors: TaskWizardAccessEntry[];
  /** The statuses of the ancestors' entries with their number, empty ones left out. */
  ancestorCounts: [TaskWizardAccessStatus, number][];
}

/**
 * Sorts the entries of a result into one block per group, so the ancestors can be reported as one line
 * instead of one per level of the tree.
 */
export function groupEntries(
  entries: TaskWizardAccessEntry[]
): WizardResultBlock[] {
  const blocks = new Map<TaskWizardGroupType, WizardResultBlock>();
  for (const entry of entries) {
    let block = blocks.get(entry.groupType);
    if (!block) {
      block = {
        groupType: entry.groupType,
        groupName: entry.groupName,
        roleKey: ROLE_KEYS[entry.groupType],
        ancestors: [],
        ancestorCounts: [],
      };
      blocks.set(entry.groupType, block);
    }
    if (entry.pickedElement) {
      block.picked = entry;
    } else {
      block.ancestors.push(entry);
    }
  }
  for (const block of blocks.values()) {
    block.ancestorCounts = STATUS_ORDER.map(
      (status) =>
        [status, block.ancestors.filter((e) => e.status === status).length] as [
          TaskWizardAccessStatus,
          number,
        ]
    ).filter(([, count]) => count > 0);
  }
  return [...blocks.values()];
}
