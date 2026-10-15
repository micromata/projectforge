"use client";

import type { ReactNode } from "react";
import { useTranslations } from "next-intl";
import { renderCell, type CellSpec } from "@/components/data-table";
import { useFormatContext } from "@/hooks/use-format";
import { TASK_METADATA } from "@/lib/metadata/task.generated";
import type { TaskListRow } from "./types";

/**
 * The three cells the task list shares with the task tree, adapted to a column declaration.
 *
 * The renderers live in `components/data-table/cells/` and are reached through [renderCell], which
 * takes the *dynamic grid's* shape: a column's render instruction arrives there as a serialisable
 * `CellSpec` from the layout response. A declared column has no such spec, so each wrapper writes the
 * literal its renderer needs and hands over the row. Adapting here rather than widening the renderers
 * keeps one implementation per cell — the two perspectives of a task cannot paint the same value
 * differently.
 */

/** Every spec here is a constant, so no wrapper allocates one per render. */
const CONSUMPTION: CellSpec = { kind: "consumption" };
const ORDERS: CellSpec = { kind: "orders" };
const TASK_STATUS: CellSpec = { kind: "taskStatus" };

/** The two context values every one of the cells reads, plus the row widened to what they expect. */
function useCell(row: TaskListRow) {
  const t = useTranslations();
  const ctx = useFormatContext();
  return {
    row: row as unknown as Record<string, unknown>,
    ctx,
    // The cells name their keys at runtime (an enum constant, an accessible name), which next-intl's
    // literal key type cannot express — the same widening `useDeclaredColumns` does.
    t: t as unknown as (key: string) => string,
  };
}

export function TaskConsumptionCell({ row }: { row: TaskListRow }): ReactNode {
  return renderCell({
    spec: CONSUMPTION,
    value: row.consumption,
    ...useCell(row),
  });
}

export function TaskOrdersCell({ row }: { row: TaskListRow }): ReactNode {
  return renderCell({ spec: ORDERS, value: row.orderList, ...useCell(row) });
}

/**
 * The status, coloured by the raw enum letter and worded by the bundle.
 *
 * The tree gets both from the backend (`statusAsString` beside the raw `status`); a list row carries
 * the letter only, so the text comes from the same `@PropertyInfo` constants the form's select offers
 * rather than from a second list of words.
 */
export function TaskStatusListCell({ row }: { row: TaskListRow }): ReactNode {
  const cell = useCell(row);
  const constant = TASK_METADATA.fields.status.enumValues.find(
    (value) => value.value === row.status
  );
  if (!constant) return null;
  return renderCell({
    spec: TASK_STATUS,
    value: cell.t(constant.i18nKey),
    ...cell,
  });
}
