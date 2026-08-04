"use client";

import { HugeiconsIcon } from "@hugeicons/react";
import { PlusSignIcon, Search01Icon } from "@hugeicons/core-free-icons";
import Link from "next/link";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";

interface BooksToolbarProps {
  search: string;
  onSearch: (v: string) => void;
  /** Column visibility/pinning panel, rendered once the table instance exists. */
  columnPanel?: React.ReactNode;
}

export function BooksToolbar({
  search,
  onSearch,
  columnPanel,
}: BooksToolbarProps) {
  return (
    <div className="border-b bg-background">
      <div className="flex items-center gap-3 px-4 pt-3">
        <div>
          <p className="text-[11px] font-medium uppercase tracking-wider text-muted-foreground">
            Projektmanagement
          </p>
          <h1 className="text-lg font-bold tracking-tight">Bücherliste</h1>
        </div>
        <div className="flex-1" />
        {columnPanel}
        <Button asChild size="sm" className="gap-1.5">
          <Link href="/books/new">
            <HugeiconsIcon icon={PlusSignIcon} size={13} />
            Hinzufügen
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
            placeholder="Bücherliste durchsuchen…"
            className="h-9 pl-9"
          />
        </div>
      </div>

    </div>
  );
}
