"use client";

import type { ReactNode } from "react";
import Link from "next/link";
import { HugeiconsIcon } from "@hugeicons/react";
import { PlusSignIcon, Search01Icon } from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Separator } from "@/components/ui/separator";
import { LegacyPageLink } from "@/components/shared/legacy-page-link";

export interface ListToolbarProps {
  /** Heading of the page, e.g. "Bücherliste". */
  title: string;
  /** The menu parent above it, e.g. "Allgemein" — where the entry sits in the main menu. */
  category: string;
  searchValue: string;
  onSearchChange: (value: string) => void;
  searchPlaceholder: string;
  /** Route of the add page, e.g. `/books/new`. */
  addHref: string;
  addLabel: string;
  /** The legacy list page (`ui.legacyUrl` of the list response), see LegacyPageLink. */
  legacyUrl?: string;
  /** Column visibility/pinning panel, rendered once the table instance exists. */
  columnPanel?: ReactNode;
  /** Active filters as editable pills plus the "all filters" trigger. */
  filterPills?: ReactNode;
  /** Maintenance actions of the list (re-index, reset filter), see ListGearMenu. */
  gearMenu?: ReactNode;
}

/** The head of every list page: where it sits, what it is called, search, filters and "add". */
export function ListToolbar({
  title,
  category,
  searchValue,
  onSearchChange,
  searchPlaceholder,
  addHref,
  addLabel,
  legacyUrl,
  columnPanel,
  filterPills,
  gearMenu,
}: ListToolbarProps) {
  return (
    <div className="border-b bg-background">
      <div className="flex items-center gap-3 px-4 pt-3">
        <LegacyPageLink url={legacyUrl} className="-ml-1" />
        <div>
          <p className="text-[11px] font-medium uppercase tracking-wider text-muted-foreground">
            {category}
          </p>
          <h1 className="text-lg font-bold tracking-tight">{title}</h1>
        </div>
        <div className="flex-1" />
        {/* Divider only with a menu beside it: it separates the list's own actions from
            "add", which creates an entity. */}
        {gearMenu && (
          <>
            {gearMenu}
            <Separator orientation="vertical" className="!h-5" />
          </>
        )}
        <Button asChild size="sm" className="gap-1.5">
          <Link href={addHref}>
            <HugeiconsIcon icon={PlusSignIcon} size={13} />
            {addLabel}
          </Link>
        </Button>
      </div>

      <div className="flex items-center gap-3 px-4 py-2.5">
        <div className="relative max-w-md flex-1">
          <HugeiconsIcon
            icon={Search01Icon}
            size={14}
            className="absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground"
          />
          <Input
            value={searchValue}
            onChange={(e) => onSearchChange(e.target.value)}
            placeholder={searchPlaceholder}
            aria-label={searchPlaceholder}
            className="h-9 pl-9"
          />
        </div>
      </div>

      {/* Filters left, table settings right: both act on the list below, not on the page. */}
      {(filterPills || columnPanel) && (
        <div className="flex items-start gap-3 px-4 pb-2.5">
          <div className="flex-1">{filterPills}</div>
          {columnPanel}
        </div>
      )}
    </div>
  );
}
