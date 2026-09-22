import type {
  TaskWizardAccessEntry,
  TaskWizardGroupType,
  TaskWizardResult,
} from "@/lib/rs/task";
import { ROLE_KEYS } from "./result-model";

/** The order the columns are shown in — the order of the steps above the table (see GROUP_STEPS). */
const COLUMN_ORDER: TaskWizardGroupType[] = ["TEAM", "MANAGER", "EXTERNAL"];

/** One group that was picked, i.e. one column of the preview. */
export interface WizardPreviewColumn {
  groupType: TaskWizardGroupType;
  groupName?: string | null;
  /** The role's name, `task.wizard.result.role.*`. */
  roleKey: string;
}

/**
 * One structure element of the path, i.e. one row of the preview.
 *
 * @param indent Its depth below the topmost element shown, which is where the row is indented by.
 * @param cells The entry per group, keyed by its role; a group that has no entry on this element has none.
 */
export interface WizardPreviewRow {
  taskId: number;
  taskTitle?: string | null;
  indent: number;
  pickedElement: boolean;
  cells: Partial<Record<TaskWizardGroupType, TaskWizardAccessEntry>>;
}

export interface WizardPreview {
  columns: WizardPreviewColumn[];
  rows: WizardPreviewRow[];
}

/**
 * Turns the answer of the preview into the table: one row per element of the hierarchy, one column per
 * group.
 *
 * The hierarchy comes out of the order of the entries, not out of a depth the backend sends: the rows of
 * a group are the one path from the picked element up to the root (`TaskWizardService.createAccessRights`
 * recurses into the parent), so that chain reversed is the path top down, and the position in it is the
 * indentation. The groups all walk the same chain, hence the longest one decides the rows and the others
 * are matched onto it by element.
 */
export function buildPreview(result: TaskWizardResult): WizardPreview {
  const chains = new Map<TaskWizardGroupType, TaskWizardAccessEntry[]>();
  for (const entry of result.entries) {
    const chain = chains.get(entry.groupType);
    if (chain) {
      chain.push(entry);
    } else {
      chains.set(entry.groupType, [entry]);
    }
  }
  const columns = COLUMN_ORDER.filter((groupType) => chains.has(groupType)).map(
    (groupType) => ({
      groupType,
      groupName: chains.get(groupType)![0].groupName,
      roleKey: ROLE_KEYS[groupType],
    })
  );
  const longest = [...chains.values()].reduce<TaskWizardAccessEntry[]>(
    (longestSoFar, chain) =>
      chain.length > longestSoFar.length ? chain : longestSoFar,
    []
  );
  const rows: WizardPreviewRow[] = [...longest]
    .reverse()
    .map((entry, index) => ({
      taskId: entry.taskId,
      taskTitle: entry.taskTitle,
      indent: index,
      pickedElement: entry.pickedElement,
      cells: {},
    }));
  const byTaskId = new Map(rows.map((row) => [row.taskId, row]));
  for (const entry of result.entries) {
    const row = byTaskId.get(entry.taskId);
    if (row) {
      row.cells[entry.groupType] = entry;
    }
  }
  return { columns, rows };
}
