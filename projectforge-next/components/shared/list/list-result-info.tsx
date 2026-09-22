"use client";

import ReactMarkdown from "react-markdown";
import remarkGfm from "remark-gfm";
import rehypeRaw from "rehype-raw";
import { cn } from "@/lib/utils";

interface ListResultInfoProps {
  /**
   * The backend's `ResultSet.resultInfo` — markdown that may carry raw HTML (a red `<span>` for a
   * truncated result, list markup for statistics). Rendered as-is, the way the legacy React list does,
   * so a page inherits whatever note the backend attaches without knowing its shape.
   */
  info: string;
  /** Wrapper classes — the caller owns the box (sky note on a dynamic page, plain strip under a table). */
  className?: string;
}

/**
 * Renders the backend's `resultInfo` markdown. Shared by the server-laid-out list page (its sky note)
 * and the hand built lists (a strip under the table), so both render the same server text the same way.
 *
 * `rehypeRaw` on purpose: `resultInfo` contains raw HTML from the backend (the truncation `<span>`),
 * which plain markdown would escape into visible tags. The source is our own backend, not user input.
 */
export function ListResultInfo({ info, className }: ListResultInfoProps) {
  return (
    <div className={cn("text-sm", className)}>
      <ReactMarkdown
        remarkPlugins={[remarkGfm]}
        rehypePlugins={[rehypeRaw]}
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
