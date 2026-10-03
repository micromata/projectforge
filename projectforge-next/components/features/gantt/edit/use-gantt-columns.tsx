"use client";

import { useMemo } from "react";
import { useTranslations } from "next-intl";
import type { ColumnDef } from "@tanstack/react-table";
import type { GanttRow } from "../gantt-tree";
import {
  GanttDateCell,
  GanttEnumCell,
  GanttNumberCell,
  GanttVisibleCell,
} from "./gantt-input-cells";
import { GanttPredecessorCell } from "./gantt-predecessor-cell";
import { GanttTitleCell } from "./gantt-title-cell";

const RELATION_TYPES = [
  "START_START",
  "START_FINISH",
  "FINISH_START",
  "FINISH_FINISH",
] as const;
const OBJECT_TYPES = ["ACTIVITY", "SUMMARY", "MILESTONE"] as const;

function column(
  id: string,
  header: string,
  size: number,
  cell: (row: GanttRow) => React.ReactNode
): ColumnDef<GanttRow, unknown> {
  return {
    id,
    header,
    size,
    enableSorting: false,
    meta: { label: header },
    cell: ({ row }) => cell(row.original),
  };
}

/**
 * The columns of the Gantt tree table (Wicket's GanttChartEditTreeTablePanel). The cells read the tree from
 * the editor context, so the definitions stay the same while the tree is edited.
 */
export function useGanttColumns(): ColumnDef<GanttRow, unknown>[] {
  const t = useTranslations();
  return useMemo(() => {
    const relationLabels = {
      START_START: t("gantt.relationType.start_start"),
      START_FINISH: t("gantt.relationType.start_finish"),
      FINISH_START: t("gantt.relationType.finish_start"),
      FINISH_FINISH: t("gantt.relationType.finish_finish"),
    };
    const typeLabels = {
      ACTIVITY: t("gantt.objectType.activity"),
      SUMMARY: t("gantt.objectType.summary"),
      MILESTONE: t("gantt.objectType.milestone"),
    };
    return [
      column("visible", "", 36, (row) => <GanttVisibleCell row={row} />),
      column("title", t("title"), 340, (row) => <GanttTitleCell row={row} />),
      column("startDate", t("gantt.startDate"), 150, (row) => (
        <GanttDateCell row={row} field="START_DATE" />
      )),
      column("duration", t("gantt.duration"), 100, (row) => (
        <GanttNumberCell
          row={row}
          field="DURATION"
          labelKey="gantt.duration"
          fractionDigits={2}
        />
      )),
      column("endDate", t("gantt.endDate"), 150, (row) => (
        <GanttDateCell row={row} field="END_DATE" />
      )),
      column("progress", t("task.progress"), 90, (row) => (
        <GanttNumberCell row={row} field="PROGRESS" labelKey="task.progress" />
      )),
      column("predecessor", t("gantt.predecessor"), 220, (row) => (
        <GanttPredecessorCell row={row} />
      )),
      column("predecessorOffset", t("gantt.predecessorOffset"), 90, (row) => (
        <GanttNumberCell
          row={row}
          field="PREDECESSOR_OFFSET"
          labelKey="gantt.predecessorOffset"
        />
      )),
      column("relationType", t("gantt.relationType.short"), 120, (row) => (
        <GanttEnumCell
          row={row}
          field="RELATION_TYPE"
          labelKey="gantt.relationType.short"
          values={RELATION_TYPES}
          labels={relationLabels}
        />
      )),
      column("type", t("gantt.objectType.short"), 120, (row) => (
        <GanttEnumCell
          row={row}
          field="TYPE"
          labelKey="gantt.objectType.short"
          values={OBJECT_TYPES}
          labels={typeLabels}
        />
      )),
    ];
  }, [t]);
}
