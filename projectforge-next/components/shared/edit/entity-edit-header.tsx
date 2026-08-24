"use client";

import type { ReactNode } from "react";
import { HugeiconsIcon } from "@hugeicons/react";
import { ArrowLeft01Icon } from "@hugeicons/core-free-icons";
import { GuardedLink } from "@/components/shared/guarded-link";
import { LegacyPageLink } from "@/components/shared/legacy-page-link";

export interface EntityEditHeaderProps {
  /** The menu parent of the entity, e.g. "Common" — the same eyebrow its list page carries. */
  category: string;
  /** Where the breadcrumb leads, e.g. `/cost1`. */
  listRoute: string;
  /** Label of that link, e.g. the list's title. */
  listLabel: string;
  /** The entry being edited, e.g. a book's title or a cost number. */
  title: string;
  /** The legacy React edit page of this entry, see LegacyPageLink. */
  legacyUrl?: string;
  /** Anything the entity wants beside its title — a book's loan badge. */
  trailing?: ReactNode;
  /** What else can be done with this entry, as one menu — see EntityCrossLinks. */
  crossLinks?: ReactNode;
}

/**
 * The head of every edit page: where the entity sits in the menu, the way back to its list, and what
 * is being edited.
 *
 * The category above the breadcrumb, from the same `categoryKey` the list page reads: an edit page is
 * a page of the application like any other and says where it stands (see PageTitleRow).
 */
export function EntityEditHeader({
  category,
  listRoute,
  listLabel,
  title,
  legacyUrl,
  trailing,
  crossLinks,
}: EntityEditHeaderProps) {
  return (
    <div className="border-b border-border bg-background px-6 pb-1.5 pt-2">
      <p className="text-[11px] font-medium uppercase tracking-wider text-muted-foreground">
        {category}
      </p>
      <div className="flex items-center gap-2 overflow-hidden">
        {/* Guarded: this is the way out of a form that may hold unsaved changes. */}
        <GuardedLink
          href={listRoute}
          className="flex shrink-0 items-center gap-1 text-sm font-medium text-foreground/70 hover:text-foreground"
        >
          <HugeiconsIcon
            icon={ArrowLeft01Icon}
            size={14}
            className="text-muted-foreground"
          />
          <span>{listLabel}</span>
        </GuardedLink>
        <span className="shrink-0 text-base text-border">/</span>
        <span className="min-w-0 flex-1 truncate text-sm text-muted-foreground">
          {title}
        </span>
        {trailing}
        {crossLinks}
        {/* Right, not left: the left of this row is the breadcrumb back to the list, and putting a
            second link beside it would read as part of that path. */}
        <LegacyPageLink url={legacyUrl} />
      </div>
    </div>
  );
}
