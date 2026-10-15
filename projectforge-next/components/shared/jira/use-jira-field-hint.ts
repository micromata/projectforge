"use client";

import { useTranslations } from "next-intl";
import { useAuth } from "@/hooks/use-auth";

/**
 * The field hint that tells the user a free-text field understands JIRA issue keys — the explanation
 * Wicket puts on the help icon `FieldsetPanel.addJIRAField` adds beside such a field
 * (`tooltip.jiraSupport.field.content`).
 *
 * Not a JIRA-specific widget: it feeds the generic `hint` of any field (rendered by [FieldHint] via
 * [FieldShell]), so the ⓘ, the tooltip and the layout are the same as every other field hint. All that
 * is JIRA's own is the text and the condition — like [JiraIssuesLinks], the hint exists only where JIRA
 * is configured, so a field on an instance with no JIRA to link to says nothing about it.
 *
 * @param enabled whether this field supports JIRA at all; `false`/omitted yields no hint even when JIRA
 *   is configured, so a caller can gate a declaration's flag through here uniformly.
 * @returns the hint text, or `undefined` when the field has no JIRA hint (pass straight to `hint`).
 */
export function useJiraFieldHint(enabled?: boolean): string | undefined {
  const t = useTranslations();
  const { jira } = useAuth();
  return enabled && jira?.configured
    ? t("tooltip.jiraSupport.field.content")
    : undefined;
}
