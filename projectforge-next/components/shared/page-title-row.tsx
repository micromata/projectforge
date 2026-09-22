"use client";

import type { ReactNode } from "react";
import { LegacyPageLink } from "@/components/shared/legacy-page-link";

export interface PageTitleRowProps {
  /** The menu parent above the title, e.g. "Structure tree" — where the page sits in the main menu.
      Omit it when it would only repeat the title (e.g. the calendar's "Kalender" over "Kalender"). */
  category?: string;
  /** Heading of the page, e.g. "List view". */
  title: string;
  /** The legacy page this one replaces (`ui.legacyUrl` of a list response), see LegacyPageLink. */
  legacyUrl?: string;
  /** Fills the flexible space between the title and the actions — a header-level control such as the
      calendar's chooser field. Left empty it is just the spacer that pushes the actions right. */
  center?: ReactNode;
  /** The page's own actions, right of the legacy link. */
  children?: ReactNode;
}

/**
 * The head row of a page: where it sits, what it is called, the way back to Wicket, and the actions.
 *
 * One component for the list toolbar and the structure tree page, which are two perspectives on the
 * same tasks and looked like two different applications while each built its own header (see
 * ListToolbar and app/(authenticated)/taskTree).
 */
export function PageTitleRow({
  category,
  title,
  legacyUrl,
  center,
  children,
}: PageTitleRowProps) {
  return (
    <div className="flex items-center gap-3 px-4 pt-3">
      <div className="shrink-0">
        {category && (
          <p className="text-[11px] font-medium uppercase tracking-wider text-muted-foreground">
            {category}
          </p>
        )}
        <h1 className="text-lg font-bold tracking-tight">{title}</h1>
      </div>
      {/* The flexible middle: a header control if the page gives one, otherwise a plain spacer that
          pushes the actions to the right edge. `min-w-0` lets an input here shrink instead of overflow. */}
      <div className="min-w-0 flex-1">{center}</div>
      {/* `self-start`: the actions line up with the top of the two-line title block rather than
          with its middle. Their own row keeps them centered among themselves. */}
      <div className="flex items-center gap-3 self-start">
        {/* Leftmost of the actions: it leaves the page, the ones to its right act on it. */}
        <LegacyPageLink url={legacyUrl} />
        {children}
      </div>
    </div>
  );
}
