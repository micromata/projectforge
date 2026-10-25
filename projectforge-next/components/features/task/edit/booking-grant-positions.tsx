"use client";

import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { Delete02Icon } from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import { ValueCombobox } from "@/components/shared/value-combobox";
import type { TaskBookingGrantableTask } from "@/lib/rs/task-booking-grant";
import { BookingGrantKost2Select } from "./booking-grant-kost2-select";

/** A task of the grant to give, with its picked cost units (none: all). */
export interface BookingGrantPositionValue {
  taskId: number;
  kost2Ids: number[];
}

/**
 * The structure elements of a booking grant: the task of the form and any of its bookable sub elements
 * (offered by the backend, see `TaskBookingGrantRest.Info.tasks`), each with its own cost units.
 */
export function BookingGrantPositions({
  tasks,
  truncated,
  value,
  onChange,
}: {
  tasks: TaskBookingGrantableTask[];
  truncated: boolean;
  value: BookingGrantPositionValue[];
  onChange: (value: BookingGrantPositionValue[]) => void;
}) {
  const t = useTranslations();
  const byId = new Map(tasks.map((task) => [task.taskId, task]));
  const chosen = new Set(value.map((position) => position.taskId));
  const addable = tasks.filter((task) => !chosen.has(task.taskId));
  const update = (taskId: number, kost2Ids: number[]) =>
    onChange(
      value.map((position) =>
        position.taskId === taskId ? { taskId, kost2Ids } : position
      )
    );

  return (
    <div className="flex flex-col gap-2">
      {value.map((position) => {
        const task = byId.get(position.taskId);
        const path = task?.path ?? String(position.taskId);
        return (
          <div
            key={position.taskId}
            className="flex flex-col gap-1 rounded-md border p-2 sm:flex-row sm:items-center sm:gap-2"
          >
            <span className="min-w-0 flex-1 text-sm break-words">{path}</span>
            {task?.kost2List && task.kost2List.length > 0 && (
              <div className="sm:w-64">
                <BookingGrantKost2Select
                  label={`${t("fibu.kost2._")}: ${path}`}
                  kost2List={task.kost2List}
                  value={position.kost2Ids}
                  onChange={(kost2Ids) => update(position.taskId, kost2Ids)}
                />
              </div>
            )}
            <Button
              type="button"
              variant="outline"
              size="icon"
              aria-label={`${t("delete")}: ${path}`}
              onClick={() =>
                onChange(value.filter((it) => it.taskId !== position.taskId))
              }
            >
              <HugeiconsIcon icon={Delete02Icon} size={16} />
            </Button>
          </div>
        );
      })}
      {addable.length > 0 && (
        <ValueCombobox
          aria-label={t("task.bookingGrant.positions.add")}
          options={addable.map((task) => ({
            value: String(task.taskId),
            label: task.path,
          }))}
          selected={[]}
          onChange={(values) =>
            values[0] &&
            onChange([...value, { taskId: Number(values[0]), kost2Ids: [] }])
          }
          placeholder={t("task.bookingGrant.positions.add")}
          className="w-full"
        />
      )}
      {truncated && (
        <p className="text-xs text-muted-foreground">
          {t("task.bookingGrant.positions.truncated", { arg0: tasks.length })}
        </p>
      )}
    </div>
  );
}
