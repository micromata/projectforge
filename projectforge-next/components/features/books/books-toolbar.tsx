"use client";

import { HugeiconsIcon } from "@hugeicons/react";
import { PlusSignIcon, Search01Icon } from "@hugeicons/core-free-icons";
import { useTranslations } from "next-intl";
import Link from "next/link";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Separator } from "@/components/ui/separator";

interface BooksToolbarProps {
  search: string;
  onSearch: (v: string) => void;
  /** Column visibility/pinning panel, rendered once the table instance exists. */
  columnPanel?: React.ReactNode;
  /** Active filters as editable pills plus the "all filters" trigger. */
  filterPills?: React.ReactNode;
  /** Maintenance actions of the list (re-index, reset filter), see ListGearMenu. */
  gearMenu?: React.ReactNode;
}

export function BooksToolbar({
  search,
  onSearch,
  columnPanel,
  filterPills,
  gearMenu,
}: BooksToolbarProps) {
  const t = useTranslations();

  return (
    <div className="border-b bg-background">
      <div className="flex items-center gap-3 px-4 pt-3">
        <div>
          <p className="text-[11px] font-medium uppercase tracking-wider text-muted-foreground">
            {t("menu.common")}
          </p>
          <h1 className="text-lg font-bold tracking-tight">
            {t("books.title")}
          </h1>
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
          <Link href="/books/new">
            <HugeiconsIcon icon={PlusSignIcon} size={13} />
            {t("book.title.add")}
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
            value={search}
            onChange={(e) => onSearch(e.target.value)}
            placeholder={t("books.searchPlaceholder")}
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
