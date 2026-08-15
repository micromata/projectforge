"use client";

import type { ReactElement } from "react";
import {
  Tooltip,
  TooltipContent,
  TooltipTrigger,
} from "@/components/ui/tooltip";
import { MarkdownText } from "@/components/shared/markdown-text";

/**
 * The one explanatory tooltip of the app.
 *
 * Everything that explains an element on hover goes through this, so that all of them share a look,
 * a delay and a maximum width. Before this there were two kinds: the styled Radix one and the
 * browser's native `title`, which renders as a grey OS box, appears after a second, cannot wrap where
 * the text wants to and is announced by nothing.
 *
 * The explanation is markdown ([MarkdownText]): the bundle is already written in it, and the long
 * texts need paragraphs, emphasis and the occasional list to be readable at all (see
 * `fibu.auftrag.probabilityOfOccurrence.weighted.info`, which is three sentences). `text-[11px]`
 * because a tooltip is a footnote — it explains what is already on the page.
 *
 * No `TooltipProvider`: `app/layout.tsx` has the single one for the whole app.
 *
 * Not an accessible name — an icon-only trigger still needs its own `aria-label`.
 */
export function HintTooltip({
  title,
  text,
  plain,
  side,
  children,
}: {
  /**
   * What the element *is*, above the explanation — for a trigger whose own label is an icon
   * ("Neuen Eintrag anlegen" above the keyboard shortcut, see [ListToolbar]).
   */
  title?: string;
  /** Nothing is rendered without one, so a caller may pass an optional backend tooltip as it is. */
  text?: string | null;
  /**
   * Renders [text] verbatim instead of as markdown — for content from the database or from the user
   * (a remark, an entity name), where an underscore is an underscore and not emphasis.
   */
  plain?: boolean;
  side?: "top" | "right" | "bottom" | "left";
  /** The element the tooltip explains; it becomes the trigger itself (`asChild`). */
  children: ReactElement;
}) {
  if (!text && !title) return children;
  return (
    <Tooltip>
      <TooltipTrigger asChild>{children}</TooltipTrigger>
      {/* `flex-col items-start`: the primitive lays its children out as a centered row, which would
          put the title beside its explanation instead of above it. */}
      <TooltipContent
        side={side}
        className="max-w-sm flex-col items-start gap-1 text-[11px] leading-relaxed"
      >
        {title && <span className="font-semibold">{title}</span>}
        {text &&
          (plain ? (
            // `whitespace-pre-wrap`: the line breaks of the value itself are all its structure.
            <span className="whitespace-pre-wrap">{text}</span>
          ) : (
            <MarkdownText text={text} />
          ))}
      </TooltipContent>
    </Tooltip>
  );
}
