"use client";

import type { ReactNode } from "react";
import { HugeiconsIcon } from "@hugeicons/react";
import { ArrowLeft01Icon } from "@hugeicons/core-free-icons";
import { GuardedLink } from "@/components/shared/guarded-link";
import { LegacyPageLink } from "@/components/shared/legacy-page-link";

export interface EntityEditHeaderProps {
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

/** The top row of every edit page: the way back to the list, what is being edited, and where it was. */
export function EntityEditHeader({
  listRoute,
  listLabel,
  title,
  legacyUrl,
  trailing,
  crossLinks,
}: EntityEditHeaderProps) {
  return (
    <div className="flex h-11 items-center gap-2 overflow-hidden border-b border-border bg-background px-6">
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
  );
}
