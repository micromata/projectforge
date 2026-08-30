import type { JiraConfig } from "@/lib/rs/types";

/**
 * Issue-key pattern, mirroring the backend's `JiraUtils.PATTERN` (`[A-Z][A-Z_0-9]*-[0-9]+`): an
 * uppercase project prefix, a dash, a number — e.g. `PROJECTFORGE-222`. Global so every occurrence is
 * found; not anchored, so a key inside a sentence still matches, exactly as Wicket's `linkJiraIssues`.
 */
const JIRA_ISSUE_PATTERN = /[A-Z][A-Z_0-9]*-[0-9]+/g;

/** The project prefix of an issue key (`PROJECTFORGE-222` → `PROJECTFORGE`). */
function projectOf(issueKey: string): string {
  return issueKey.slice(0, issueKey.indexOf("-"));
}

/**
 * The browse url for one issue key, mirroring the backend's `JiraUtils.getJiraBrowseBaseUrl` +
 * `buildJiraIssueBrowseLinkUrl`: the base url of the server whose projects the key starts with (case
 * matched as the backend uppercases both), else the default browse base url — with the key appended
 * directly, since the configured base url already ends with the browse path. Null where JIRA is off or
 * no base url fits, so the caller renders plain text.
 */
export function buildJiraIssueUrl(
  issueKey: string,
  config: JiraConfig | null | undefined
): string | null {
  if (!config?.configured) return null;
  const project = projectOf(issueKey);
  const server = config.servers?.find((s) => s.projects.includes(project));
  const baseUrl = server?.baseUrl ?? config.defaultBrowseBaseUrl;
  return baseUrl ? `${baseUrl}${issueKey}` : null;
}

/** One piece of a text split at its issue keys: either plain text or a matched issue key. */
export type JiraSegment =
  | { type: "text"; value: string }
  | { type: "issue"; value: string };

/**
 * Splits a text into plain-text runs and the issue keys between them, in order, so a renderer can turn
 * only the keys into links and leave the rest untouched. A text with no key yields a single text
 * segment; an empty/blank text yields none.
 */
export function splitJiraSegments(
  text: string | null | undefined
): JiraSegment[] {
  if (!text) return [];
  const segments: JiraSegment[] = [];
  let lastIndex = 0;
  // A fresh regex per call: the shared literal keeps `lastIndex` across calls and would skip matches.
  const pattern = new RegExp(JIRA_ISSUE_PATTERN);
  let match: RegExpExecArray | null;
  while ((match = pattern.exec(text)) !== null) {
    if (match.index > lastIndex) {
      segments.push({
        type: "text",
        value: text.slice(lastIndex, match.index),
      });
    }
    segments.push({ type: "issue", value: match[0] });
    lastIndex = match.index + match[0].length;
  }
  if (lastIndex < text.length) {
    segments.push({ type: "text", value: text.slice(lastIndex) });
  }
  return segments;
}

/** The unique issue keys a text contains, in first-seen order — for the links-below-a-field row. */
export function findJiraIssues(text: string | null | undefined): string[] {
  if (!text) return [];
  const seen = new Set<string>();
  for (const m of text.matchAll(JIRA_ISSUE_PATTERN)) {
    seen.add(m[0]);
  }
  return [...seen];
}
