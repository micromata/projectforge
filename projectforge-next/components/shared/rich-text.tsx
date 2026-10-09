"use client";

import { useMemo } from "react";
import DOMPurify from "dompurify";
import { looksLikeMarkdown, markdownToHtml } from "@/lib/markdown-to-html";
import { cn } from "@/lib/utils";

/** The markup [RichTextEditor] produces; everything else is removed. */
const ALLOWED_TAGS = [
  "p",
  "br",
  "h1",
  "h2",
  "h3",
  "h4",
  "strong",
  "b",
  "em",
  "i",
  "u",
  "s",
  "ul",
  "ol",
  "li",
  "a",
  "span",
  "table",
  "thead",
  "tbody",
  "tr",
  "th",
  "td",
];
const ALLOWED_ATTR = ["href", "style", "target", "rel", "colspan", "rowspan"];

let hooksInstalled = false;

/**
 * Keeps nothing of a `style` but its `color` (the only style the editor writes), and opens every link
 * in a new tab without access to its opener.
 */
function installHooks() {
  if (hooksInstalled) return;
  hooksInstalled = true;
  DOMPurify.addHook("uponSanitizeAttribute", (_node, data) => {
    if (data.attrName !== "style") return;
    const color = /(?:^|;)\s*color\s*:\s*([#\w(),.\s%-]+?)\s*(?:;|$)/i.exec(
      data.attrValue
    )?.[1];
    if (color) {
      data.attrValue = `color: ${color}`;
    } else {
      data.keepAttr = false;
    }
  });
  DOMPurify.addHook("afterSanitizeAttributes", (node) => {
    if (node.tagName === "A") {
      node.setAttribute("target", "_blank");
      node.setAttribute("rel", "noopener noreferrer");
    }
  });
}

/** The stored HTML of a rich text, sanitized; empty where there is no DOM (prerendering). */
export function sanitizeRichText(html: string): string {
  if (!DOMPurify.isSupported) return "";
  installHooks();
  return DOMPurify.sanitize(html, { ALLOWED_TAGS, ALLOWED_ATTR });
}

/**
 * Whether a stored text is HTML written by [RichTextEditor]: its documents always open with a block (a
 * paragraph, a heading, a list or a table). False for a text from before it was rich text (plain text
 * or markdown, maybe with inline HTML).
 */
export function isRichTextHtml(text: string): boolean {
  return /^\s*<(p|h[1-4]|ul|ol|table)[\s>]/i.test(text);
}

/**
 * A stored text as the HTML of a rich text, not yet sanitized: the editor's HTML as it is, any other
 * text (plain text or markdown) converted. So a markdown text shows formatted until it is saved again
 * in the editor, which then writes HTML.
 */
export function toRichTextHtml(text: string): string {
  return !text.trim() || isRichTextHtml(text) ? text : markdownToHtml(text);
}

/**
 * The body text of a rich text muted, so bold text (full foreground colour) stands out against it in
 * light and dark mode — the weight alone hardly does. Bold text inside a coloured span keeps that
 * colour (the more specific `span[style]` rule). For the alert and note boxes and the editor itself.
 */
export const RICH_TEXT_MUTED_CLASSES =
  "text-muted-foreground [&_:is(h1,h2,h3,h4,th)]:text-foreground [&_b]:text-foreground [&_strong]:text-foreground [&_span[style]_b]:text-inherit [&_span[style]_strong]:text-inherit";

/**
 * The text of a rich text without its markup, the blocks (paragraphs, list items, line breaks) joined
 * by a space: for a one-line preview (a list cell). Empty where there is no DOM (prerendering).
 */
export function richTextToPlainText(text: string): string {
  if (!/<[a-z][\s\S]*>/i.test(text) && !looksLikeMarkdown(text)) return text;
  if (typeof DOMParser === "undefined") return "";
  // A parsed document runs no scripts and loads nothing, so it needs no sanitizing for its text.
  const spaced = toRichTextHtml(text).replace(
    /<\/(p|li|h[1-4]|th|td)>|<br\s*\/?>/gi,
    (tag) => `${tag} `
  );
  const plain = new DOMParser().parseFromString(spaced, "text/html").body
    .textContent;
  return (plain ?? "").replace(/\s+/g, " ").trim();
}

/**
 * A rich text of [RichTextEditor] (HTML from the database), rendered sanitized: only the markup the
 * editor writes survives (paragraphs, headings, emphasis, lists, tables, links, text colour), no
 * scripts, no event handlers. A text written before it was rich text (plain text or markdown) is
 * converted first, see [toRichTextHtml].
 */
export function RichText({
  html,
  className,
}: {
  html: string;
  className?: string;
}) {
  const sanitized = useMemo(
    () => sanitizeRichText(toRichTextHtml(html)),
    [html]
  );
  return (
    <div
      className={cn(
        // font-normal: the body is font-medium, against which bold text would hardly stand out.
        "rich-text space-y-1.5 font-normal [&_a]:underline [&_li]:ml-1 [&_ol]:list-decimal [&_ol]:space-y-0.5 [&_ol]:pl-4 [&_strong]:font-semibold [&_ul]:list-disc [&_ul]:space-y-0.5 [&_ul]:pl-4",
        className
      )}
      dangerouslySetInnerHTML={{ __html: sanitized }}
    />
  );
}
