"use client";

import ReactMarkdown from "react-markdown";
import remarkGfm from "remark-gfm";
import rehypeRaw from "rehype-raw";
import rehypeSanitize, { defaultSchema } from "rehype-sanitize";
import { cn } from "@/lib/utils";

interface ListResultInfoProps {
  /**
   * The backend's `ResultSet.resultInfo` — markdown that may carry raw HTML (a red `<span>` for a
   * truncated result, list markup for statistics). Rendered sanitized, so a page inherits whatever note
   * the backend attaches without knowing its shape.
   */
  info: string;
  /** Wrapper classes — the caller owns the box (sky note on a dynamic page, plain strip under a table). */
  className?: string;
}

/**
 * Inline style of a span: only colour and font weight (the truncation note's red, bold span in ResultSet.kt).
 * Any other css (e.g. `position:fixed`) could lay a fake element over the page.
 */
const SPAN_STYLE = /^\s*(?:(?:color|font-weight)\s*:\s*[a-z0-9#]+\s*;?\s*)+$/i;

/** The default schema of rehype-sanitize, plus a restricted inline style of a span. */
const SANITIZE_SCHEMA = {
  ...defaultSchema,
  attributes: {
    ...defaultSchema.attributes,
    span: [...(defaultSchema.attributes?.span ?? []), ["style", SPAN_STYLE]],
  },
};

/**
 * Renders the backend's `resultInfo` markdown. Shared by the server-laid-out list page (its sky note)
 * and the hand built lists (a strip under the table), so both render the same server text the same way.
 *
 * `rehypeRaw` on purpose: `resultInfo` contains raw HTML from the backend (the truncation `<span>`),
 * which plain markdown would escape into visible tags. Sanitized afterwards anyway: the texts may
 * carry user input one day (an entry's name in a note), and then a raw `<img onerror>` would run.
 */
export function ListResultInfo({ info, className }: ListResultInfoProps) {
  return (
    <div className={cn("text-sm", className)}>
      <ReactMarkdown
        remarkPlugins={[remarkGfm]}
        rehypePlugins={[rehypeRaw, [rehypeSanitize, SANITIZE_SCHEMA]]}
        components={{
          ul: ({ children }) => (
            <ul className="list-disc space-y-1 pl-4">{children}</ul>
          ),
          li: ({ children }) => (
            <li className="text-muted-foreground">{children}</li>
          ),
        }}
      >
        {info}
      </ReactMarkdown>
    </div>
  );
}
