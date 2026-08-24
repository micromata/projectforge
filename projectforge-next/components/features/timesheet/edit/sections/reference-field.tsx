"use client";

import { useStore } from "@tanstack/react-form";
import { useTranslations } from "next-intl";
import { StringSuggestField } from "@/components/shared/form/string-suggest-field";
import { useEntityEditForm } from "@/components/shared/form/form-context";
import { fetchReferenceSuggestions } from "@/lib/rs/timesheet";
import type { TimesheetEditValues } from "../timesheet-edit-schema";

/**
 * A way of grouping sheets across a task and its subtasks (`timesheet.reference.info`), completing from
 * the references already used there. Narrowed by the current task, so the suggestions follow it: the task
 * id is part of the query key, and changing the task re-queries (and the backend answers over all tasks
 * while none is chosen).
 */
export function ReferenceField({ className }: { className?: string }) {
  const t = useTranslations();
  const form = useEntityEditForm();
  const taskId = useStore(
    form.store,
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    (state: any) => (state.values as TimesheetEditValues).task?.id ?? null
  ) as number | null;

  return (
    <StringSuggestField
      name="reference"
      label={t("timesheet.reference")}
      className={className}
      suggest={(search, signal) =>
        fetchReferenceSuggestions(search, taskId, signal)
      }
      queryKey={["timesheet", "reference", taskId]}
    />
  );
}
