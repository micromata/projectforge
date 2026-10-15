import type { GanttValues } from "./gantt-schema";
import type { GanttDiagramDetail } from "./types";

/**
 * A field Spring left out of the JSON (`JsonInclude.Include.NON_NULL`) arrives as `undefined`; every
 * value is normalised here, so no field ever holds `undefined`.
 */
export function toFormValues(chart: GanttDiagramDetail): GanttValues {
  return {
    id: chart.id ?? null,
    name: chart.name ?? null,
    task: chart.task ?? null,
    owner: chart.owner ?? null,
    readAccessType: chart.readAccessType ?? "OWNER",
    writeAccessType: chart.writeAccessType ?? "OWNER",
    title: chart.title ?? null,
    fromDate: chart.fromDate ?? null,
    toDate: chart.toDate ?? null,
    showOnlyVisibles: chart.showOnlyVisibles ?? false,
    openNodes: chart.openNodes ?? null,
    width: chart.width ?? null,
    totalLabelWidth: chart.totalLabelWidth ?? null,
    relativeTimeValues: chart.relativeTimeValues ?? false,
    showToday: chart.showToday ?? false,
    showCompletion: chart.showCompletion ?? false,
    root: chart.root ?? null,
  };
}

/**
 * Placeholder until the new-entry preset arrives — the backend's `newEntry` fills owner, access and the
 * style defaults (GanttChartEntityRest.newBaseDO), which the form then takes over.
 */
export function emptyGanttValues(): GanttValues {
  return toFormValues({ id: null });
}
