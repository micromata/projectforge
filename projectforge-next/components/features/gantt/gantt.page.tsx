import { GANTT_CHART_METADATA } from "@/lib/metadata/gantt-chart.generated";
import { definePage } from "@/lib/page-def/define-page";
import { GanttChartPreview } from "./edit/gantt-chart-preview";
import { GanttExportMenu } from "./edit/gantt-export-menu";
import {
  GanttLabelWidthField,
  GanttOptionsField,
  GanttPeriodField,
  GanttReadAccessField,
  GanttTitleField,
  GanttWidthField,
  GanttWriteAccessField,
} from "./edit/gantt-settings-fields";
import { GanttTreeSection } from "./edit/gantt-tree-section";
import { ganttSchema, GANTT_FIELDS, type GanttValues } from "./gantt-schema";
import { emptyGanttValues, toFormValues } from "./gantt-values";
import type { GanttDiagramDetail, GanttDiagramListRow } from "./types";

/** React Query key of the list, so a write from the edit page refreshes it. */
export const GANTT_LIST_QUERY_KEY = ["gantt"] as const;

/**
 * The Gantt chart page — list and edit — as data (see lib/page-def/types.ts), replacing Wicket's
 * GanttChartListPage / GanttChartEditPage. The list shows Wicket's columns. The edit page has the chart's
 * settings, the editable object tree and the chart as the server draws it, with its exports.
 */
export const GANTT_PAGE = definePage<
  GanttDiagramListRow,
  GanttValues,
  GanttDiagramDetail,
  typeof GANTT_CHART_METADATA
>({
  entity: "gantt",
  metadata: GANTT_CHART_METADATA,
  route: "/gantt",
  queryKey: GANTT_LIST_QUERY_KEY,
  // Where the entry sits in the main menu: Project management > Gantt (MenuItemDefId.GANTT).
  categoryKey: "menu.projectmanagement",
  titleKey: "gantt.title.list",
  // Name descending, as Wicket's GanttChartListPage sorted.
  defaultSort: { id: "name", desc: true },
  columns: [
    { name: "name", size: 260, className: "font-semibold" },
    {
      name: "task",
      size: 260,
      // The plain task title with the path to the root as the tooltip, as the project list shows it.
      cell: ({ row }) =>
        row.original.task?.title ?? row.original.task?.displayName ?? null,
      tooltip: (row) => row.task?.path ?? undefined,
    },
    {
      name: "owner",
      size: 180,
      cell: ({ row }) => row.original.owner?.displayName ?? null,
    },
    { name: "created", size: 130 },
    { name: "lastUpdate", size: 130 },
  ],
  edit: {
    schema: ganttSchema,
    fieldNames: GANTT_FIELDS,
    defaultValues: emptyGanttValues,
    toFormValues,
    title: (chart) => chart.name ?? "",
    newTitleKey: "gantt.title.add",
    savedMessageKey: "message.successfullChanged",
    clone: true,
    // The task page's "Create Gantt diagram" passes its task (GanttChartEntityRest.newEntry).
    newEntryParams: ["task"],
    sections: [
      {
        id: "settings",
        titleKey: "label.options",
        fields: [
          { name: "task", span: 2 },
          { name: "name" },
          { name: "owner" },
          { custom: GanttTitleField, span: 2 },
          { custom: GanttReadAccessField },
          { custom: GanttWriteAccessField },
          { custom: GanttPeriodField, span: 2 },
          { custom: GanttWidthField },
          { custom: GanttLabelWidthField },
          { custom: GanttOptionsField, span: 2 },
        ],
      },
      {
        id: "tree",
        titleKey: "gantt.title.heading",
        render: () => <GanttTreeSection />,
      },
      {
        id: "preview",
        titleKey: "preview",
        headerActions: GanttExportMenu,
        render: () => <GanttChartPreview />,
      },
    ],
  },
});
