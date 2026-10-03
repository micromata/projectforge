import { z } from "zod";
import { GANTT_CHART_METADATA } from "@/lib/metadata/gantt-chart.generated";
import { fromMetadata } from "@/lib/validation/from-metadata";
import { maxMarker, minMarker, REQUIRED } from "@/lib/validation/markers";
import type { GanttObject } from "./types";

/**
 * Name, task and owner are GanttChartDO properties, so their rules come from the generated metadata.
 * Everything else is serialized into the entity's style and settings XML (GanttChartStyle,
 * GanttChartSettings) and has no metadata — those rules mirror the Wicket form (GanttChartEditForm).
 */
const m = fromMetadata(GANTT_CHART_METADATA);

/** A number of the style, bounded the way the Wicket form bounds it, with the backend's wording. */
function bounded(min: number, max: number) {
  return z
    .number()
    .nullable()
    .refine((v) => v == null || v >= min, minMarker(min))
    .refine((v) => v == null || v <= max, maxMarker(max));
}

const access = z.enum(["OWNER", "PROJECT_MANAGER", "ALL"]).nullable();

export const ganttSchema = z.object({
  id: z.number().nullable(),
  name: m.nullableString("name"),
  // The chart is a view of the task's sub tree, so it has none without one (GanttChartEntityRest.validate).
  task: m.entityField("task").refine((v): boolean => v != null, REQUIRED),
  owner: m.entityField("owner"),
  readAccessType: access,
  writeAccessType: access,
  title: z.string().nullable(),
  fromDate: z.string().nullable(),
  toDate: z.string().nullable(),
  showOnlyVisibles: z.boolean(),
  openNodes: z.array(z.number()).nullable(),
  // The Wicket form's bounds (MinMaxNumberField 100..10000 and 10..10000).
  width: bounded(100, 10000),
  totalLabelWidth: bounded(10, 10000),
  relativeTimeValues: z.boolean(),
  showToday: z.boolean(),
  showCompletion: z.boolean(),
  /** The edited tree, sent along on save (GanttChartDao.writeGanttObjects keeps only its differences). */
  root: z.custom<GanttObject | null>(),
});

export type GanttValues = z.infer<typeof ganttSchema>;

/**
 * Field names of the form, so a server validation error can be checked against what actually renders
 * (see applyServerValidationErrors) instead of vanishing into a field nobody sees.
 */
export const GANTT_FIELDS = Object.keys(
  ganttSchema.shape
) as readonly (keyof GanttValues)[];
