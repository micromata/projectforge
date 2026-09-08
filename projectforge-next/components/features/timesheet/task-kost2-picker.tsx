"use client";

import { useEffect, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import { renderCell, type CellSpec } from "@/components/data-table";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { FieldShell, useFieldIds } from "@/components/shared/form/field-shell";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import { TaskSelectControl } from "@/components/shared/tasks/task-select-control";
import { TaskSelectModal } from "@/components/shared/tasks/task-select-modal";
import type { EntityRef } from "@/components/shared/entity-autocomplete";
import { useFormatContext } from "@/hooks/use-format";
import { fetchTaskInfo } from "@/lib/rs/task";
import { cn } from "@/lib/utils";

/** The bar is the table's cell renderer, reached with the literal spec it needs (see TaskConsumptionCell). */
const CONSUMPTION: CellSpec = { kind: "consumption" };

/**
 * What a time sheet is booked on: the task and the cost unit it allows — the `timesheet.edit.taskAndKost2`
 * widget of the legacy form, form-agnostic so both the edit form (TaskKost2Section) and the mass update
 * (TaskKost2MassUpdateField) drive it from their own state.
 *
 * The two belong together because the task decides the cost unit. Which cost units may be booked is a
 * property of the task (`TaskNode.kost2List`), so the select only exists where that list is not empty.
 * Picking another task keeps the chosen cost unit *when the new task allows it too* — a shared cost unit
 * is a common case (sibling tasks of the same project), and dropping it would make the caller re-pick the
 * same value. It is dropped only when the new task's list no longer contains it, since that combination
 * is one the backend refuses (`timesheet.error.invalidKost2`). Whether one is *required* stays the
 * server's answer (`timesheet.error.kost2Required`): it depends on the task's project, which the client
 * does not reason about — the edit form passes `required` in, the mass update leaves it to the server.
 *
 * The reconciliation is reactive rather than done at pick time: the new task's `kost2List` is not on the
 * reference [onTaskChange] hands back, it arrives with the `["taskInfo", id]` query below, so keeping-or-
 * dropping can only be decided once that answer is in. `["taskInfo", id]` is the same query every other
 * view of a task by its id uses, so this is a cache read wherever the tree has already asked.
 */
export function TaskKost2Picker({
  taskId,
  kost2Id,
  onTaskChange,
  onKost2Change,
  required,
  taskErrors = [],
  kost2Errors = [],
  showConsumption = false,
  disabled,
  className,
}: {
  taskId: number | null;
  kost2Id: number | null;
  onTaskChange: (task: EntityRef | null) => void;
  onKost2Change: (kost2Id: number | null) => void;
  /** Marks the cost unit field as mandatory — the edit form's server rule; off for the mass update. */
  required?: boolean;
  taskErrors?: string[];
  kost2Errors?: string[];
  /** Show what is already booked on the task; off where a single figure carries no meaning (mass update). */
  showConsumption?: boolean;
  disabled?: boolean;
  className?: string;
}) {
  const t = useTranslations();
  const format = useFormatContext();
  const taskIds = useFieldIds();
  const kost2Ids = useFieldIds();
  // Where the tree opens rooted; reset when the dialog closes (see TaskSelectField for the reasoning).
  const [open, setOpen] = useState(false);
  const [rootAtId, setRootAtId] = useState<number | null>(null);

  const { data: info } = useQuery({
    queryKey: ["taskInfo", taskId],
    queryFn: ({ signal }) => fetchTaskInfo(taskId!, signal),
    enabled: taskId != null,
    staleTime: Infinity,
  });

  const kost2List = info?.kost2List ?? [];

  // Reconcile the cost unit against the task's list: keep it while the (new) task still allows it, drop it
  // otherwise. `info == null` means the list for the current task is not in yet — leave the value be until
  // it is, or a shared cost unit would blink out and back during the fetch. A task with no cost units
  // allows none, so its empty list clears any leftover value.
  useEffect(() => {
    if (kost2Id == null) return;
    if (taskId == null) {
      onKost2Change(null);
      return;
    }
    if (info == null) return;
    if (!kost2List.some((kost2) => kost2.id === kost2Id)) {
      onKost2Change(null);
    }
    // `kost2List` is derived from `info`; keying on `info` avoids a new array identity re-running this.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [taskId, info, kost2Id]);

  /** The reference the callers expect, built from the node the tree hands back. */
  const change = (task: { id: number; title?: string } | null) =>
    onTaskChange(
      task != null ? { id: task.id, displayName: task.title ?? "" } : null
    );

  return (
    <div
      className={cn(
        "grid grid-cols-1 gap-x-6 gap-y-4 md:grid-cols-3",
        className
      )}
    >
      <FieldShell
        label={t("task._")}
        required={false}
        invalid={taskErrors.length > 0}
        errors={taskErrors}
        ids={taskIds}
        className="md:col-span-3"
      >
        <TaskSelectControl
          taskId={taskId}
          ariaLabel={t("task._")}
          disabled={disabled}
          onOpen={() => {
            setRootAtId(null);
            setOpen(true);
          }}
          onSelect={change}
          // Clicking a segment of the path opens the tree scoped there — the drill-down the legacy form had.
          openTreeOnAncestorClick
          onDrillDown={(task) => {
            setRootAtId(task.id);
            setOpen(true);
          }}
        />
        <TaskSelectModal
          value={taskId}
          rootTaskId={rootAtId}
          onChange={change}
          open={open}
          onOpenChange={(next) => {
            setOpen(next);
            if (!next) setRootAtId(null);
          }}
        />
      </FieldShell>

      {/* Only where the task has cost units at all: on a task without them the select would be an empty
          dropdown next to a field the backend never asks for. */}
      {kost2List.length > 0 && (
        <FieldShell
          label={t("fibu.kost2._")}
          required={required}
          invalid={kost2Errors.length > 0}
          errors={kost2Errors}
          ids={kost2Ids}
        >
          <Select
            value={kost2Id != null ? String(kost2Id) : ""}
            disabled={disabled}
            onValueChange={(value) => {
              if (value === "") return;
              onKost2Change(Number(value));
            }}
          >
            <SelectTrigger
              id={kost2Ids.controlId}
              aria-labelledby={kost2Ids.labelId}
              className="min-w-0 flex-1"
            >
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              {/* The number as the backend formatted it, which is what a cost unit is called. */}
              {kost2List.map((kost2) => (
                <SelectItem key={kost2.id} value={String(kost2.id)}>
                  {kost2.title}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </FieldShell>
      )}

      {/* What is already booked on this task, as everywhere else it is shown — the same bar, linking to the
          sheets behind it. Only where the caller asks for it (the edit form). */}
      {showConsumption && info?.consumption != null && (
        <div className="flex flex-col items-start gap-1.5 md:col-start-3">
          <span className="text-[11.5px] font-semibold uppercase tracking-wide text-muted-foreground">
            {t("task.consumption")}
          </span>
          <HintTooltip
            plain
            text={(info.consumption as { title?: string }).title}
          >
            <div className="w-full max-w-[220px]">
              {renderCell({
                spec: CONSUMPTION,
                value: info.consumption,
                row: info as unknown as Record<string, unknown>,
                ctx: format,
                // The cells name their keys at runtime, which next-intl's literal key type cannot express.
                t: t as unknown as (key: string) => string,
              })}
            </div>
          </HintTooltip>
        </div>
      )}
    </div>
  );
}
