"use client";

import type { ReactNode } from "react";
import { ExperimentalBadge } from "@/components/shared/experimental-badge";
import { useDocumentTitle } from "@/hooks/use-document-title";
import { cn } from "@/lib/utils";

export interface PageTitleRowProps {
  /** The menu parent above the title, e.g. "Structure tree" — where the page sits in the main menu.
      Omit it when it would only repeat the title (e.g. the calendar's "Kalender" over "Kalender"). */
  category?: string;
  /** Heading of the page, e.g. "List view". */
  title: string;
  /** The page belongs to an unfinished feature: a large red badge beside the title (see ExperimentalBadge). */
  experimental?: boolean;
  /** The text of that badge instead of "Experimental", e.g. "Demo" for a prototype on test data. */
  experimentalLabel?: string;
  /** Fills the flexible space between the title and the actions — a header-level control such as the
      calendar's chooser field. Left empty it is just the spacer that pushes the actions right. */
  center?: ReactNode;
  /** Beside the title, e.g. a pill marking what the page is about. */
  titleAddon?: ReactNode;
  /** The page's own actions. */
  children?: ReactNode;
  /** Too narrow for title, center and actions in one line: the actions move to a line of their own instead of
      being covered by the center, which keeps the width of its content (tabs that don't shrink). */
  wrap?: boolean;
  /** Below sm the center takes a full-width line of its own under title and actions, instead of being squeezed
      between them — for a field that grows, such as the calendar chooser. */
  centerBelow?: boolean;
  /** On a cramped screen (a phone) the row takes one line: no category, the title truncates, and the actions
      become items of the row itself, so one of them can take a line of its own below (`cramped:basis-full`,
      see ListToolbar). There is no [center] then. */
  compactWhenCramped?: boolean;
}

/**
 * The head row of a page: where it sits, what it is called, and the actions.
 *
 * One component for the list toolbar and the structure tree page, which are two perspectives on the
 * same tasks and looked like two different applications while each built its own header (see
 * ListToolbar and app/(authenticated)/taskTree).
 */
export function PageTitleRow({
  category,
  title,
  experimental,
  experimentalLabel,
  center,
  titleAddon,
  children,
  wrap = false,
  centerBelow = false,
  compactWhenCramped = false,
}: PageTitleRowProps) {
  useDocumentTitle(title);
  const titleClass = cn(
    "text-lg leading-6 font-bold tracking-tight",
    compactWhenCramped && "cramped:truncate"
  );
  return (
    <div
      className={cn(
        // Below sm the actions may not fit beside the title: they wrap onto their own line (right-aligned,
        // see below) instead of sticking out of the screen.
        "@container flex items-center gap-3 px-3 pt-2 max-sm:flex-wrap max-sm:gap-y-2 sm:px-4 sm:pt-2",
        wrap && "flex-wrap gap-y-2",
        centerBelow && "max-sm:flex-wrap max-sm:gap-y-2",
        compactWhenCramped && "cramped:flex-wrap cramped:gap-y-2"
      )}
    >
      <div
        className={cn(
          "shrink-0",
          // A zero basis: in the wrapping row a title as wide as its text would push the actions onto the next
          // line before it shrank at all.
          compactWhenCramped && "cramped:min-w-0 cramped:flex-1"
        )}
      >
        {category && (
          // Not printed: where the page sits in the menu says nothing on paper.
          <p
            className={cn(
              "text-[11px] leading-3 font-medium uppercase tracking-wider text-muted-foreground print:hidden",
              compactWhenCramped && "cramped:hidden"
            )}
          >
            {category}
          </p>
        )}
        {titleAddon ? (
          <div className="flex min-w-0 items-center gap-2">
            <h1 className={titleClass}>{title}</h1>
            {titleAddon}
          </div>
        ) : (
          <h1 className={titleClass}>{title}</h1>
        )}
      </div>
      {experimental && <ExperimentalBadge label={experimentalLabel} />}
      {/* The flexible middle: a header control if the page gives one, otherwise a plain spacer that
          pushes the actions to the right edge. `min-w-0` lets an input here shrink instead of overflow. */}
      <div
        className={cn(
          "min-w-0",
          wrap ? "flex-[1_1_auto]" : "flex-1",
          centerBelow && "max-sm:order-last max-sm:basis-full",
          // The title fills the line itself (no center then).
          compactWhenCramped && "cramped:hidden"
        )}
      >
        {center}
      </div>
      {/* `self-start`: the actions line up with the top of the two-line title block rather than
          with its middle. Their own row keeps them centered among themselves. Not printed: nothing on
          paper can be clicked. */}
      <div
        className={cn(
          "flex flex-wrap items-center gap-2 self-start max-sm:ml-auto sm:gap-3 print:hidden",
          wrap && "ml-auto",
          compactWhenCramped && "cramped:contents"
        )}
      >
        {children}
      </div>
    </div>
  );
}
