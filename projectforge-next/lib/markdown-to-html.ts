import { Marked } from "marked";

/**
 * GitHub flavoured markdown (tables, strike-through, autolinks), a single line break kept as one: the
 * texts written before there was a rich text editor (tooltips, notes) were meant line by line.
 */
const markdown = new Marked({ gfm: true, breaks: true });

/**
 * A markdown text as HTML. Inline HTML is passed through (an old text may colour a date with a
 * `<span style="color: …">`), so the result is not safe: always sanitize it (see `sanitizeRichText`).
 * Plain text comes back as paragraphs, i.e. a plain text is a markdown text without markup.
 */
export function markdownToHtml(text: string): string {
  return markdown.parse(text, { async: false }).trim();
}

/** Headings, lists, block quotes, table rows, emphasis, code and links: what a markdown text is told by. */
const MARKDOWN_PATTERN =
  /^\s{0,3}(#{1,6}\s|[-*+]\s|\d+[.)]\s|>|\|.*\|\s*$)|\*\*[^*\n]+\*\*|__[^_\n]+__|`[^`\n]+`|\[[^\]\n]+\]\([^)\s]+\)/m;

/** Whether a text carries markdown markup, i.e. wasn't meant as plain text (e.g. on a paste). */
export function looksLikeMarkdown(text: string): boolean {
  return MARKDOWN_PATTERN.test(text);
}
