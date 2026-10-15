"use client";

import { TemplatesRecentBarView } from "../edit/sections/templates-recent-bar";
import type { TimesheetDetail } from "../types";
import { useTimesheetTemplateList } from "../use-timesheet-template-list";
import type { BookDaysValues } from "./types";

/**
 * The user's templates and last time sheets above the booking fields, as in the time sheet edit form.
 * Both only fill the *what* (task, cost unit, location, description) and never the period; a template
 * saved here stores that *what* as well.
 */
export function BookDaysTemplates({
  values,
  onApply,
}: {
  values: BookDaysValues;
  onApply: (template: TimesheetDetail) => void;
}) {
  const current = (): TimesheetDetail => ({
    id: null,
    user: values.user ? { id: values.user.id } : null,
    task: values.taskId != null ? { id: values.taskId } : null,
    kost2: values.kost2Id != null ? { id: values.kost2Id } : null,
    location: values.location || null,
    description: values.description || null,
  });
  return (
    <TemplatesRecentBarView
      templates={useTimesheetTemplateList(current, onApply)}
      compact
    />
  );
}
