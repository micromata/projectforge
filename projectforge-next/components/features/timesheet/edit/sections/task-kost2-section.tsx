"use client";

import { useEffect } from "react";
import { useStore } from "@tanstack/react-form";
import { useQuery } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import { renderCell, type CellSpec } from "@/components/data-table";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import { useEntityEditForm } from "@/components/shared/form/form-context";
import { SelectField } from "@/components/shared/form/select-field";
import { TaskSelectField } from "@/components/shared/tasks/task-select-field";
import { useFormatContext } from "@/hooks/use-format";
import { fetchTaskInfo } from "@/lib/rs/task";
import { cn } from "@/lib/utils";
import type { TimesheetEditValues } from "../timesheet-edit-schema";

/** The bar is the table's cell renderer, reached with the literal spec it needs (see TaskConsumptionCell). */
const CONSUMPTION: CellSpec = { kind: "consumption" };

/**
 * What the sheet is booked on: the task, the cost unit it allows, and how much of that task is used up —
 * the `timesheet.edit.taskAndKost2` and `task.consumption` widgets of the legacy form in one card.
 *
 * The three belong together because the task decides the other two. Which cost units may be booked is a
 * property of the task (`TaskNode.kost2List`), so the select only exists where that list is not empty.
 * Picking another task keeps the chosen cost unit *when the new task allows it too* — a shared cost unit
 * is a common case (sibling tasks of the same project), and dropping it would make the user re-pick the
 * same value. It is dropped only when the new task's list no longer contains it, since that combination
 * is one the backend refuses (`timesheet.error.invalidKost2`). Whether one is *required* stays the
 * server's answer (`timesheet.error.kost2Required`): it depends on the task's project, which the client
 * does not reason about.
 *
 * The reconciliation is reactive rather than done at pick time: the new task's `kost2List` is not on the
 * reference the picker hands back, it arrives with the `["taskInfo", id]` query below, so keeping-or-
 * dropping can only be decided once that answer is in.
 *
 * `["taskInfo", id]` is the same query every other view of a task by its id uses (TaskChip,
 * TaskSelectField, the task form's cost unit block), so this is a cache read wherever the tree has
 * already asked.
 */
export function TaskKost2Section({ className }: { className?: string }) {
  const t = useTranslations();
  const format = useFormatContext();
  const form = useEntityEditForm();
  const taskId = useStore(
    form.store,
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    (state: any) => (state.values as TimesheetEditValues).task?.id ?? null
  ) as number | null;

  const { data: info } = useQuery({
    queryKey: ["taskInfo", taskId],
    queryFn: ({ signal }) => fetchTaskInfo(taskId!, signal),
    enabled: taskId != null,
    staleTime: Infinity,
  });

  const kost2Id = useStore(
    form.store,
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    (state: any) => (state.values as TimesheetEditValues).kost2?.id ?? null
  ) as number | null;

  const kost2List = info?.kost2List ?? [];

  // Reconcile the cost unit against the task's list: keep it while the (new) task still allows it, drop
  // it otherwise. `info == null` means the list for the current task is not in yet — leave the value be
  // until it is, or a shared cost unit would blink out and back during the fetch. A task with no cost
  // units (`costConfigured` false) allows none, so its empty list clears any leftover value.
  useEffect(() => {
    if (kost2Id == null) return;
    // Task cleared: nothing allows a cost unit, so drop it right away (no list to wait for).
    if (taskId == null) {
      form.setFieldValue("kost2", null);
      return;
    }
    if (info == null) return;
    if (!kost2List.some((kost2) => kost2.id === kost2Id)) {
      form.setFieldValue("kost2", null);
    }
    // `kost2List` is derived from `info`; keying on `info` avoids a new array identity re-running this.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [taskId, info, kost2Id, form]);

  return (
    <div
      className={cn(
        "grid grid-cols-1 gap-x-6 gap-y-4 md:grid-cols-3",
        className
      )}
    >
      {/* Whether the cost unit survives the new task is decided reactively once its list arrives — see
          the effect above — not here, where the new task's `kost2List` is not yet known. */}
      <TaskSelectField
        name="task"
        label={t("task._")}
        className="md:col-span-3"
        // Clicking a segment of the path opens the tree scoped there, so the booking points below a
        // structure element are one click away — the drill-down the legacy timesheet form had.
        openTreeOnAncestorClick
      />
      {/* Only where the task has cost units at all: on a task without them the select would be an empty
          dropdown next to a field the backend never asks for. */}
      {kost2List.length > 0 && (
        <SelectField
          name="kost2"
          label={t("fibu.kost2._")}
          // The number as the backend formatted it, which is what a cost unit is called; the title it
          // sends is exactly that (`Kost2.copyFromMinimal` → `formattedNumber`, plus its description).
          options={kost2List.map((kost2) => ({
            value: String(kost2.id),
            label: kost2.title,
          }))}
          valueType="entityRef"
        />
      )}
      {/* What is already booked on this task, as everywhere else it is shown — the same bar, linking to
          the sheets behind it. A picture only while no task is chosen. */}
      {info?.consumption != null && (
        <div className="flex flex-col items-start gap-1.5 md:col-start-3">
          <span className="text-[11.5px] font-semibold uppercase tracking-wide text-muted-foreground">
            {t("task.consumption")}
          </span>
          {/* The track is `width:100%`, so on its own row it would run the whole form width — bound it to
              the compact width it has in the tree. The delegated table tooltip does not reach a bar shown
              on its own (it listens on a DataTable), so the pre-rendered figures ride an explicit tooltip
              here instead; `plain`, since the "0,00PT/225PT (0%)" text is a formatted value, not markdown. */}
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
                // The cells name their keys at runtime, which next-intl's literal key type cannot express —
                // the same widening the task list's cell wrappers do.
                t: t as unknown as (key: string) => string,
              })}
            </div>
          </HintTooltip>
        </div>
      )}
    </div>
  );
}
