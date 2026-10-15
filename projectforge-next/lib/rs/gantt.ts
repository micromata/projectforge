/**
 * The calls of the Gantt editor beside the entity's own list/read/write (`/rs/gantt`): the services of
 * `GanttServicesRest` (`/rs/ganttServices`) — the object tree of a task, the server-rendered preview,
 * the exports and the actions writing a Gantt object back to its structure element.
 */

import { rawRequest, request, RsError } from "./client";
import { downloadPost } from "./download";
import type {
  GanttDiagramDetail,
  GanttObject,
  GanttTaskValues,
} from "@/components/features/gantt/types";

const BASE = "/rs/ganttServices";

/** The plain Gantt tree of a task, for a chart whose task was just chosen (`GET objects`). */
export function fetchGanttObjects(
  taskId: number,
  signal?: AbortSignal
): Promise<GanttObject | null> {
  return request<GanttObject | null>(
    `${BASE}/objects?taskId=${taskId}`,
    { method: "GET" },
    signal
  );
}

/** The start and end the chart calculates for a node (from its predecessor, duration, children). */
export interface GanttCalculatedDates {
  start?: string | null;
  end?: string | null;
}

/** `GanttServicesRest.Preview`: the chart as SVG (null: nothing to draw) and the calculated dates by id. */
export interface GanttPreview {
  svg?: string | null;
  dates: Record<string, GanttCalculatedDates>;
}

/** Renders the posted, unsaved chart (`POST preview`). Writes nothing. */
export function fetchGanttPreview(
  chart: GanttDiagramDetail,
  signal?: AbortSignal
): Promise<GanttPreview> {
  return request<GanttPreview>(
    `${BASE}/preview`,
    { method: "POST", body: JSON.stringify(chart) },
    signal
  );
}

/** `GanttServicesRest.ExportFormat`. */
export type GanttExportFormat =
  | "PDF"
  | "PNG"
  | "JPG"
  | "SVG"
  | "MS_PROJECT_MPX"
  | "MS_PROJECT_XML"
  | "PROJECTFORGE";

/** Downloads the posted chart in the given format. A 404 means there is nothing to draw. */
export function downloadGanttExport(
  chart: GanttDiagramDetail,
  format: GanttExportFormat
): Promise<void> {
  return downloadPost(`${BASE}/export?format=${format}`, chart);
}

/** `GanttServicesRest.TaskField`: the value of a node to write back to its structure element. */
export type GanttTaskField =
  | "TITLE"
  | "START_DATE"
  | "END_DATE"
  | "DURATION"
  | "PROGRESS"
  | "PREDECESSOR"
  | "PREDECESSOR_OFFSET"
  | "RELATION_TYPE"
  | "TYPE";

/** Writes one value of the node to its task; answers the task's values after the update. */
export function saveGanttValueToTask(
  node: GanttObject,
  field: GanttTaskField
): Promise<GanttTaskValues> {
  return request<GanttTaskValues>(`${BASE}/saveToTask`, {
    method: "POST",
    body: JSON.stringify({ node: { ...node, children: null }, field }),
  });
}

/** Moves the task below another one ("move here" / "move to top"). Answers no body. */
export async function moveGanttTask(
  taskId: number,
  parentTaskId: number
): Promise<void> {
  const path = `${BASE}/moveTask`;
  const res = await rawRequest(path, {
    method: "POST",
    body: JSON.stringify({ taskId, parentTaskId }),
  });
  if (!res.ok) {
    throw new RsError(res.status, `${res.status} ${res.statusText}: ${path}`);
  }
}

/** `GanttServicesRest.SaveAsTaskResult`: the id of the new task, which becomes the node's id. */
export interface GanttSaveAsTaskResult {
  id: number;
  task: GanttTaskValues;
}

/** Saves a Gantt-only node as a new structure element below the given task. */
export function saveGanttObjectAsTask(
  node: GanttObject,
  parentTaskId: number
): Promise<GanttSaveAsTaskResult> {
  return request<GanttSaveAsTaskResult>(`${BASE}/saveAsTask`, {
    method: "POST",
    body: JSON.stringify({ node: { ...node, children: null }, parentTaskId }),
  });
}
