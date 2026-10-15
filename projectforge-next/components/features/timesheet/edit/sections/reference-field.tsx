"use client";

import { useStore } from "@tanstack/react-form";
import { useTranslations } from "next-intl";
import { StringSuggestField } from "@/components/shared/form/string-suggest-field";
import { useJiraFieldHint } from "@/components/shared/jira/use-jira-field-hint";
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
  // The reference commonly holds a ticket number and is scanned for JIRA keys, so it carries the same
  // "supports JIRA" hint as the declarative free-text fields (see useJiraFieldHint).
  const jiraHint = useJiraFieldHint(true);

  return (
    <StringSuggestField
      name="reference"
      label={t("timesheet.reference")}
      hint={jiraHint}
      className={className}
      suggest={(search, signal) =>
        fetchReferenceSuggestions(search, taskId, signal)
      }
      queryKey={["timesheet", "reference", taskId]}
    />
  );
}
