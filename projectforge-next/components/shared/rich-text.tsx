"use client";

import { useMemo } from "react";
import DOMPurify from "dompurify";
import { cn } from "@/lib/utils";

/** The markup [RichTextEditor] produces; everything else is removed. */
const ALLOWED_TAGS = [
  "p",
  "br",
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
];
const ALLOWED_ATTR = ["href", "style", "target", "rel"];

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
 * The text of a rich text without its markup, the blocks (paragraphs, list items, line breaks) joined
 * by a space: for a one-line preview (a list cell). Empty where there is no DOM (prerendering).
 */
export function richTextToPlainText(html: string): string {
  if (!/<[a-z][\s\S]*>/i.test(html)) return html;
  if (typeof DOMParser === "undefined") return "";
  // A parsed document runs no scripts and loads nothing, so it needs no sanitizing for its text.
  const spaced = html.replace(/<\/(p|li)>|<br\s*\/?>/gi, (tag) => `${tag} `);
  const text = new DOMParser().parseFromString(spaced, "text/html").body
    .textContent;
  return (text ?? "").replace(/\s+/g, " ").trim();
}

/**
 * A rich text of [RichTextEditor] (HTML from the database), rendered sanitized: only the markup the
 * editor writes survives (paragraphs, emphasis, lists, links, text colour), no scripts, no event
 * handlers. A value without any tag (written before it was rich text) is shown as plain text.
 */
export function RichText({
  html,
  className,
}: {
  html: string;
  className?: string;
}) {
  const isHtml = /<[a-z][\s\S]*>/i.test(html);
  const sanitized = useMemo(
    () => (isHtml ? sanitizeRichText(html) : ""),
    [html, isHtml]
  );
  const classes = cn(
    "space-y-1.5 [&_a]:underline [&_li]:ml-1 [&_ol]:list-decimal [&_ol]:space-y-0.5 [&_ol]:pl-4 [&_strong]:font-semibold [&_ul]:list-disc [&_ul]:space-y-0.5 [&_ul]:pl-4",
    className
  );
  if (!isHtml) {
    return <div className={cn(classes, "whitespace-pre-wrap")}>{html}</div>;
  }
  return (
    <div className={classes} dangerouslySetInnerHTML={{ __html: sanitized }} />
  );
}
