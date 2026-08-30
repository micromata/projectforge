"use client";

import type { ComponentType } from "react";
import { useStore } from "@tanstack/react-form";
import { useEntityEditForm } from "@/components/shared/form/form-context";
import { JiraIssuesLinks } from "./jira-issues-links";

/**
 * A `custom` field for a page-def section that shows the JIRA issue keys of one text field as links
 * below it (see [JiraIssuesLinks]) — the hand-built form's equivalent of Wicket's `addJIRAField`.
 *
 * A factory rather than a component with a `name` prop because a section's `custom` slot renders a bare
 * `ComponentType<{ className?: string }>` with no channel for the field name. Called once at module load
 * where the page is declared, so the returned component is a stable reference (see task/order/timesheet
 * page defs). Reads the live value from the form store, so the links follow what the user types.
 */
export function makeJiraFieldLinks(
  fieldName: string
): ComponentType<{ className?: string }> {
  function JiraFieldLinks({ className }: { className?: string }) {
    const form = useEntityEditForm();
    const value = useStore(
      form.store,
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      (state: any) => (state.values as Record<string, unknown>)[fieldName]
    );
    return (
      <JiraIssuesLinks
        text={typeof value === "string" ? value : null}
        className={className}
      />
    );
  }
  JiraFieldLinks.displayName = `JiraFieldLinks(${fieldName})`;
  return JiraFieldLinks;
}
