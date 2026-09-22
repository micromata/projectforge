"use client";

import type { ReactElement, ReactNode } from "react";
import {
  Tooltip,
  TooltipContent,
  TooltipTrigger,
} from "@/components/ui/tooltip";
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from "@/components/ui/popover";
import { MarkdownText } from "@/components/shared/markdown-text";
import { useCoarsePointer } from "@/hooks/use-coarse-pointer";
import { cn } from "@/lib/utils";

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
 *
 * ## Touch
 *
 * A tooltip opens on hover, and a phone has none. Where the trigger is a passive element — a value
 * span, an info icon, a `<dt>` — the explanation behind it is then unreachable on iOS. Pass
 * [openOnTap] there: on a coarse pointer the very same content is rendered in a [Popover] instead,
 * which opens on tap and dismisses on a tap outside. It is left off for a trigger that is itself
 * actionable (a button, a link): a popover would swallow the tap that should run the action, and the
 * hover tooltip is a desktop bonus on those anyway — an essential explanation beside an action gets
 * its own tappable info icon (see [FieldHint]).
 */
export function HintTooltip({
  title,
  text,
  plain,
  side,
  openOnTap,
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
  /**
   * On a coarse pointer (a touch device), open on tap via a [Popover] instead of on hover. Set it
   * where the trigger is passive; leave it off where the trigger is itself actionable (see above).
   */
  openOnTap?: boolean;
  /** The element the tooltip explains; it becomes the trigger itself (`asChild`). */
  children: ReactElement;
}) {
  const coarsePointer = useCoarsePointer();
  if (!text && !title) return children;

  const body = <HintBody title={title} text={text} plain={plain} />;

  if (openOnTap && coarsePointer) {
    return (
      <Popover>
        <PopoverTrigger asChild>{children}</PopoverTrigger>
        {/* The same shape and text as the tooltip below: the only difference is the trigger, tap
            rather than hover. `w-auto` because the popover primitive is a fixed-width panel by
            default, which the footnote does not want. */}
        <PopoverContent
          side={side}
          className={cn(HINT_CONTENT_CLASS, "w-auto")}
        >
          {body}
        </PopoverContent>
      </Popover>
    );
  }

  return (
    <Tooltip>
      <TooltipTrigger asChild>{children}</TooltipTrigger>
      <TooltipContent side={side} className={HINT_CONTENT_CLASS}>
        {body}
      </TooltipContent>
    </Tooltip>
  );
}

/**
 * `flex-col items-start`: the tooltip primitive lays its children out as a centred row, which would
 * put the title beside its explanation instead of above it.
 */
const HINT_CONTENT_CLASS =
  "max-w-sm flex-col items-start gap-2 text-[11px] leading-relaxed";

/** The title and explanation, shared by the tooltip and the tap popover so they read identically. */
function HintBody({
  title,
  text,
  plain,
}: {
  title?: string;
  text?: string | null;
  plain?: boolean;
}): ReactNode {
  return (
    <>
      {title && <span className="font-semibold">{title}</span>}
      {text &&
        (plain ? (
          // `whitespace-pre-wrap`: the line breaks of the value itself are all its structure.
          <span className="whitespace-pre-wrap">{text}</span>
        ) : (
          <MarkdownText text={text} />
        ))}
    </>
  );
}
