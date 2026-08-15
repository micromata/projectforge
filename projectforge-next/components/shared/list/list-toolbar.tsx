"use client";

import type { ReactNode } from "react";
import Link from "next/link";
import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { PlusSignIcon } from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import { Separator } from "@/components/ui/separator";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import { LegacyPageLink } from "@/components/shared/legacy-page-link";
import { useAddEntryShortcut } from "@/hooks/use-add-entry-shortcut";
import { SearchInput } from "./search-input";

export interface ListToolbarProps {
  /** Heading of the page, e.g. "Bücherliste". */
  title: string;
  /** The menu parent above it, e.g. "Allgemein" — where the entry sits in the main menu. */
  category: string;
  searchValue: string;
  /** Called with the typed value once it has settled, see [SearchInput]. */
  onSearchChange: (value: string) => void;
  searchPlaceholder: string;
  /** Route of the add page, e.g. `/book/new`. */
  addHref: string;
  /** The legacy list page (`ui.legacyUrl` of the list response), see LegacyPageLink. */
  legacyUrl?: string;
  /**
   * Actions of the list itself — the exports of the order book (see PageDef.listActions). Between the
   * legacy link and the gear menu: they act on the page, but on all of it rather than on its settings.
   */
  actions?: ReactNode;
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
  legacyUrl,
  actions,
  columnPanel,
  filterPills,
  gearMenu,
}: ListToolbarProps) {
  const t = useTranslations();
  useAddEntryShortcut(addHref);
  return (
    <div className="border-b bg-background">
      <div className="flex items-center gap-3 px-4 pt-3">
        <div>
          <p className="text-[11px] font-medium uppercase tracking-wider text-muted-foreground">
            {category}
          </p>
          <h1 className="text-lg font-bold tracking-tight">{title}</h1>
        </div>
        <div className="flex-1" />
        {/* `self-start`: the actions line up with the top of the two-line title block rather than
            with its middle. Their own row keeps them centered among themselves. */}
        <div className="flex items-center gap-3 self-start">
          {/* Leftmost of the actions: it leaves the page, the ones to its right act on it. */}
          <LegacyPageLink url={legacyUrl} />
          {actions}
          {/* Divider only with a menu beside it: it separates the list's own actions from
              "add", which creates an entity. */}
          {gearMenu && (
            <>
              {gearMenu}
              {/* `!self-center`: with an explicit height the primitive's `self-stretch` degrades to
                  flex-start and hangs the line above the buttons. The `!` is needed because the
                  primitive's `data-vertical:self-stretch` carries an attribute selector and would
                  otherwise outweigh a plain `self-center`. */}
              <Separator orientation="vertical" className="!h-5 !self-center" />
            </>
          )}
          {/* With no label on the button the tooltip has to say what it does before it says how to
              reach it by keyboard. `._`: the shortcut key has a child (.title), so it is nested
              under `_` in the catalog. */}
          <HintTooltip
            title={t("menu.addNewEntry")}
            text={`${t("tooltip.shortcut.addEntry.title")}\n\n${t("tooltip.shortcut.addEntry._")}`}
          >
            {/* Icon only, and the same on every list: what is added is said by the page's own
                heading, so a label ("New book", "New order/offer") only repeats it — and reads
                badly wherever the noun has no good article. The accessible name stays generic
                for the same reason; the tooltip beside it names the shortcut. */}
            <Button asChild size="icon-lg" aria-label={t("menu.addNewEntry")}>
              <Link href={addHref}>
                {/* No `size` prop: the primitive sizes the icon with the button, and an explicit
                    width/height attribute would win over that class. */}
                <HugeiconsIcon icon={PlusSignIcon} strokeWidth={2.5} />
              </Link>
            </Button>
          </HintTooltip>
        </div>
      </div>

      <div className="flex items-center gap-3 px-4 py-2.5">
        <div className="relative max-w-md flex-1">
          <SearchInput
            value={searchValue}
            onChange={onSearchChange}
            placeholder={searchPlaceholder}
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
