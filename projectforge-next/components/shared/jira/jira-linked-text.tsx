"use client";

import { useAuth } from "@/hooks/use-auth";
import { HighlightedText } from "@/components/shared/highlighted-text";
import { buildJiraIssueUrl, splitJiraSegments } from "@/lib/jira";
import { cn } from "@/lib/utils";

/**
 * A text with its JIRA issue keys turned into links, for a list cell — the client-side equivalent of
 * Wicket's `JiraUtils.linkJiraIssues`. Where JIRA is not configured (or the key maps to no base url) the
 * key stays plain text, so the component is safe to use unconditionally.
 *
 * The links stop their click from bubbling: in a list the row itself navigates to the edit page (see
 * DataTable's `onCellClick`/row link), and a JIRA link must open JIRA instead, not both.
 */
export function JiraLinkedText({
  text,
  className,
  highlight,
}: {
  text: string | null | undefined;
  className?: string;
  /** The active search term, highlighted in the text and the link labels (see HighlightedText). */
  highlight?: string;
}) {
  const { jira } = useAuth();
  const segments = splitJiraSegments(text);
  if (segments.length === 0) return null;

  return (
    <span className={className}>
      {segments.map((segment, index) => {
        const marked = (
          <HighlightedText text={segment.value} query={highlight} />
        );
        if (segment.type === "text") return <span key={index}>{marked}</span>;
        const url = buildJiraIssueUrl(segment.value, jira);
        if (!url) return <span key={index}>{marked}</span>;
        return (
          <a
            key={index}
            href={url}
            target="_blank"
            rel="noreferrer"
            className={cn("text-primary hover:underline")}
            onClick={(event) => event.stopPropagation()}
          >
            {marked}
          </a>
        );
      })}
    </span>
  );
}
