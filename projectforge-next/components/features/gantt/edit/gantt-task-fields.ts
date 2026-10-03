import type { GanttTaskField } from "@/lib/rs/gantt";
import type { GanttObject, GanttTaskValues } from "../types";

/** A value of a Gantt object that has a counterpart in its structure element. */
export type GanttTaskKey = keyof GanttTaskValues & keyof GanttObject;

/** Which node value each save-to-task field writes (GanttServicesRest.TaskField). */
export const TASK_FIELD_KEYS: Record<GanttTaskField, GanttTaskKey> = {
  TITLE: "title",
  START_DATE: "startDate",
  END_DATE: "endDate",
  DURATION: "duration",
  PROGRESS: "progress",
  PREDECESSOR: "predecessorId",
  PREDECESSOR_OFFSET: "predecessorOffset",
  RELATION_TYPE: "relationType",
  TYPE: "type",
};

/** Whether the node's value deviates from its structure element's; never for a Gantt-only node. */
export function isModified(node: GanttObject, field: GanttTaskField): boolean {
  if (!node.task) return false;
  const key = TASK_FIELD_KEYS[field];
  return (node[key] ?? null) !== (node.task[key] ?? null);
}

/** The patch that takes the structure element's value over ("reject"). */
export function rejectPatch(
  node: GanttObject,
  field: GanttTaskField
): Partial<GanttObject> {
  const task = node.task ?? {};
  const key = TASK_FIELD_KEYS[field];
  if (field === "PREDECESSOR") {
    return {
      predecessorId: task.predecessorId ?? null,
      predecessorTitle: task.predecessorTitle ?? null,
    };
  }
  return { [key]: task[key] ?? null };
}
