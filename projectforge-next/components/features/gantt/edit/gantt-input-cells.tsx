"use client";

import { useTranslations } from "next-intl";
import { Checkbox } from "@/components/ui/checkbox";
import { SelectContent, SelectItem, SelectValue } from "@/components/ui/select";
import { Select, SelectTrigger } from "@/components/shared/copyable-select";
import { DateInput } from "@/components/shared/date-input";
import { NumberBox } from "@/components/shared/form/number-box";
import { useFormatContext } from "@/hooks/use-format";
import { formatDate, formatNumber } from "@/lib/format";
import type { GanttTaskField } from "@/lib/rs/gantt";
import type { GanttRow } from "../gantt-tree";
import { TASK_FIELD_KEYS } from "./gantt-task-fields";
import { GanttTaskValueCell } from "./gantt-task-value-cell";
import { useGanttEditorContext } from "./use-gantt-editor";

/** Radix forbids an empty item value, so "no value" needs a stand-in. */
const NONE = "__none";

export function GanttVisibleCell({ row }: { row: GanttRow }) {
  const t = useTranslations();
  const editor = useGanttEditorContext();
  return (
    <Checkbox
      checked={row.node.visible === true}
      aria-label={t("gantt.tooltip.isVisible")}
      onCheckedChange={(v) =>
        editor.update(row.node.id, { visible: v === true })
      }
    />
  );
}

/** A start or end date; while empty, the date the chart calculates is shown below. */
export function GanttDateCell({
  row,
  field,
}: {
  row: GanttRow;
  field: "START_DATE" | "END_DATE";
}) {
  const t = useTranslations();
  const ctx = useFormatContext();
  const editor = useGanttEditorContext();
  const key = field === "START_DATE" ? "startDate" : "endDate";
  const { node } = row;
  const calculated = editor.calculated[String(node.id)];
  const calc = field === "START_DATE" ? calculated?.start : calculated?.end;
  return (
    <GanttTaskValueCell
      node={node}
      field={field}
      taskValue={formatDate(node.task?.[key], ctx)}
    >
      <DateInput
        value={node[key] ?? null}
        onChange={(v) => editor.update(node.id, { [key]: v })}
        aria-label={t(
          field === "START_DATE" ? "gantt.startDate" : "gantt.endDate"
        )}
      />
      {!node[key] && calc && (
        <span className="px-1 text-[11px] text-muted-foreground">
          {formatDate(calc, ctx)}
        </span>
      )}
    </GanttTaskValueCell>
  );
}

/**
 * The bounds of Wicket's tree table (`MinMaxNumberField`; the duration's is `TaskEditForm.MAX_DURATION_DAYS`).
 * A value outside is only marked here: the tree has no form fields, so GanttChartEntityRest.validate
 * refuses the save with a message naming the activity.
 */
const BOUNDS: Partial<Record<GanttTaskField, [number, number]>> = {
  DURATION: [0, 10000],
  PROGRESS: [0, 100],
};

export function GanttNumberCell({
  row,
  field,
  labelKey,
  fractionDigits = 0,
}: {
  row: GanttRow;
  field: "DURATION" | "PROGRESS" | "PREDECESSOR_OFFSET";
  labelKey: string;
  fractionDigits?: number;
}) {
  const t = useTranslations();
  const ctx = useFormatContext();
  const editor = useGanttEditorContext();
  const key = TASK_FIELD_KEYS[field] as
    | "duration"
    | "progress"
    | "predecessorOffset";
  const { node } = row;
  const value = node[key] ?? null;
  const bounds = BOUNDS[field];
  const invalid =
    bounds != null && value != null && (value < bounds[0] || value > bounds[1]);
  return (
    <GanttTaskValueCell
      node={node}
      field={field}
      taskValue={formatNumber(node.task?.[key], ctx, fractionDigits)}
    >
      <NumberBox
        value={value}
        invalid={invalid}
        onChange={(v) => editor.update(node.id, { [key]: v })}
        fractionDigits={fractionDigits}
        maxDigits={field === "PROGRESS" ? 3 : 5}
        align="right"
        aria-label={t(labelKey)}
      />
    </GanttTaskValueCell>
  );
}

/** Relation type or Gantt type: a choice of the enum, labelled by `<prefix>.<constant in lower case>`. */
export function GanttEnumCell({
  row,
  field,
  labelKey,
  values,
  labels,
}: {
  row: GanttRow;
  field: Extract<GanttTaskField, "RELATION_TYPE" | "TYPE">;
  labelKey: string;
  values: readonly string[];
  /** The translated label of each constant. */
  labels: Record<string, string>;
}) {
  const t = useTranslations();
  const editor = useGanttEditorContext();
  const key = field === "TYPE" ? "type" : "relationType";
  const { node } = row;
  const taskValue = node.task?.[key];
  return (
    <GanttTaskValueCell
      node={node}
      field={field}
      taskValue={taskValue ? labels[taskValue] : ""}
    >
      <Select
        value={node[key] ?? NONE}
        onValueChange={(v) =>
          editor.update(node.id, { [key]: v === NONE ? null : v })
        }
      >
        <SelectTrigger className="h-7 w-full" aria-label={t(labelKey)}>
          <SelectValue />
        </SelectTrigger>
        <SelectContent>
          <SelectItem value={NONE}>–</SelectItem>
          {values.map((v) => (
            <SelectItem key={v} value={v}>
              {labels[v]}
            </SelectItem>
          ))}
        </SelectContent>
      </Select>
    </GanttTaskValueCell>
  );
}
