"use client";

import ReactMarkdown from "react-markdown";
import remarkGfm from "remark-gfm";
import rehypeRaw from "rehype-raw";
import { cn } from "@/lib/utils";

/**
 * An authored text from the i18n bundle, rendered as markdown.
 *
 * The bundle already carries markdown — `fibu.currencyPair.onlineRates.info` opens with
 * `**Automatische Wechselkurse**`, and `calendar.*.info` links with `[text](url)` — so the texts are
 * written in it whether or not it is rendered. This is where it becomes bold text, a list and a
 * paragraph instead of literal asterisks.
 *
 * Only for texts *we* wrote. Content from the database or from the user is passed through as it is
 * (an underscore in an entity name is not emphasis), so callers keep plain rendering for that.
 *
 * No raw HTML by default: a text that has to become markup is a text that belongs in a component. The
 * one exception is `allowHtml`, for an authored text an admin configures that legitimately carries HTML
 * (a time sheet's AI-savings note links with an `<a>`, mirroring the legacy `DynamicAlert`'s `rehypeRaw`)
 * — trusted, admin-only content, never something from an end user.
 */
export function MarkdownText({
  text,
  className,
  allowHtml = false,
}: {
  text: string;
  className?: string;
  allowHtml?: boolean;
}) {
  return (
    // Paragraph spacing rather than blank lines: the `\n\n` of the bundle become <p>s, and margins
    // between them read as intended everywhere the text is only a few sentences.
    <div
      className={cn(
        "space-y-1.5 [&_a]:underline [&_li]:ml-1 [&_ol]:list-decimal [&_ol]:space-y-0.5 [&_ol]:pl-4 [&_strong]:font-semibold [&_ul]:list-disc [&_ul]:space-y-0.5 [&_ul]:pl-4",
        className
      )}
    >
      <ReactMarkdown
        remarkPlugins={[remarkGfm]}
        rehypePlugins={allowHtml ? [rehypeRaw] : undefined}
      >
        {text}
      </ReactMarkdown>
    </div>
  );
}
