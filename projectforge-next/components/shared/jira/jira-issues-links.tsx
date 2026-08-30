"use client";

import { useTranslations } from "next-intl";
import { useAuth } from "@/hooks/use-auth";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import { buildJiraIssueUrl, findJiraIssues } from "@/lib/jira";
import { cn } from "@/lib/utils";

/**
 * The JIRA issue keys a text contains, listed as links below the field that holds it — the client-side
 * equivalent of Wicket's `JiraIssuesPanel` (added by `FieldsetPanel.addJIRAField`). The "JIRA" lead-in
 * carries the same explanatory tooltip Wicket puts on its help icon (`tooltip.jiraSupport.field.content`).
 *
 * Renders nothing where JIRA is not configured, the field is empty, or no key maps to a base url — so it
 * can sit unconditionally after a field.
 */
export function JiraIssuesLinks({
  text,
  className,
}: {
  text: string | null | undefined;
  className?: string;
}) {
  const t = useTranslations();
  const { jira } = useAuth();

  if (!jira?.configured) return null;
  const links = findJiraIssues(text)
    .map((issue) => ({ issue, url: buildJiraIssueUrl(issue, jira) }))
    .filter(
      (link): link is { issue: string; url: string } => link.url !== null
    );
  if (links.length === 0) return null;

  return (
    <div
      className={cn(
        "flex flex-wrap items-center gap-x-2 gap-y-1 text-sm",
        className
      )}
    >
      <HintTooltip
        title="JIRA"
        text={t("tooltip.jiraSupport.field.content")}
        plain
      >
        <span className="cursor-help text-xs font-medium text-muted-foreground">
          JIRA
        </span>
      </HintTooltip>
      {links.map(({ issue, url }) => (
        <a
          key={issue}
          href={url}
          target="_blank"
          rel="noreferrer"
          className="text-primary hover:underline"
        >
          {issue}
        </a>
      ))}
    </div>
  );
}
