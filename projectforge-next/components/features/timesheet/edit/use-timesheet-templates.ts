"use client";

import { useCallback } from "react";
import { useEntityEditForm } from "@/components/shared/form/form-context";
import type { TimesheetDetail } from "../types";
import { useTimesheetTemplateList } from "../use-timesheet-template-list";
import type { TimesheetEditValues } from "./timesheet-edit-schema";
import { templateFieldsOf, toTimesheetDetail } from "./timesheet-edit-values";

/**
 * The recent entries and saved templates of the time sheet form, and everything that fills the form
 * from one of them.
 *
 * A template — a recent sheet or a named favorite — carries the *what* of a booking and not its *when*
 * (see templateFieldsOf), so applying one leaves the period the user set.
 */
export function useTimesheetTemplates() {
  const form = useEntityEditForm();

  /** Merges a template's fields over the form, keeping the period and identity of the sheet. */
  const apply = useCallback(
    (template: TimesheetDetail) => {
      const fields = templateFieldsOf(template);
      for (const [name, value] of Object.entries(fields)) {
        form.setFieldValue(name, value);
      }
    },
    [form]
  );

  /** What the backend merges a template into: the sheet as it stands on the form. */
  const current = useCallback(
    (): TimesheetDetail =>
      toTimesheetDetail(form.state.values as TimesheetEditValues),
    [form]
  );

  return useTimesheetTemplateList(current, apply);
}
