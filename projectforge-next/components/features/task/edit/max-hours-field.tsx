"use client";

import { useTranslations } from "next-intl";
import { NumberField } from "@/components/shared/form/number-field";
import { useEntityData } from "@/components/shared/form/form-context";
import { useFieldLabels } from "@/components/shared/form/use-field-labels";
import { TASK_METADATA } from "@/lib/metadata/task.generated";
import type { TaskDetail } from "../types";

/**
 * The max hours, with Wicket's warning (`TaskEditForm`) only where it applies: an existing task with
 * order positions assigned to it or below it, whose ordered person days then win over this value
 * unless "max hours has priority" is set (`TaskTree.getPersonDays`). The flag is the server's
 * (`Task.maxHoursIgnoredDueToOrders`), read from the task the form was filled from.
 */
export function MaxHoursField({ className }: { className?: string }) {
  const t = useTranslations();
  const label = useFieldLabels(TASK_METADATA);
  const task = useEntityData<TaskDetail>();
  return (
    <NumberField
      name="maxHours"
      label={label("maxHours")}
      maxDigits={4}
      warning={
        task?.maxHoursIgnoredDueToOrders === true
          ? t("task.edit.maxHoursIngoredDueToAssignedOrders")
          : undefined
      }
      className={className}
    />
  );
}
