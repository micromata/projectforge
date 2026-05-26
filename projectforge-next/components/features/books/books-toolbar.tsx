"use client";

import { HugeiconsIcon } from "@hugeicons/react";
import {
  ArrowDown01Icon,
  PlusSignIcon,
  Search01Icon,
} from "@hugeicons/core-free-icons";
import Link from "next/link";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { SidebarTrigger } from "@/components/ui/sidebar";

interface FilterChip {
  key: string;
  label: string;
}

interface BooksToolbarProps {
  search: string;
  onSearch: (v: string) => void;
  filters: FilterChip[];
  onRemove: (key: string) => void;
  onClearAll: () => void;
}

export function BooksToolbar({
  search,
  onSearch,
  filters,
  onRemove,
  onClearAll,
}: BooksToolbarProps) {
  return (
    <div className="border-b bg-background">
      <div className="flex items-center gap-3 px-4 pt-3">
        <SidebarTrigger className="md:hidden" />
        <div>
          <p className="text-[11px] font-medium uppercase tracking-wider text-muted-foreground">
            Projektmanagement
          </p>
          <h1 className="text-lg font-bold tracking-tight">Bücherliste</h1>
        </div>
        <div className="flex-1" />
        <Button variant="outline" size="sm" className="gap-1.5">
          <span>Spalten</span>
          <HugeiconsIcon icon={ArrowDown01Icon} size={12} />
        </Button>
        <Button
          variant="outline"
          size="sm"
          className="gap-1.5 border-primary/25 bg-primary/5 text-primary hover:bg-primary/10 hover:text-primary"
        >
          <span style={{ color: "var(--brand-yellow)" }}>★</span>
          <span>Gespeichert</span>
          <HugeiconsIcon icon={ArrowDown01Icon} size={12} />
        </Button>
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

      {filters.length > 0 && (
        <div className="flex flex-wrap items-center gap-1.5 px-4 pb-2.5">
          {filters.map((f) => (
            <span
              key={f.key}
              className="inline-flex items-center gap-1 rounded-full border border-primary/30 bg-primary/10 px-2.5 py-0.5 text-xs font-bold text-primary"
            >
              {f.label}
              <button
                type="button"
                onClick={() => onRemove(f.key)}
                className="flex size-4 items-center justify-center rounded-full hover:bg-primary/20"
                aria-label={`${f.label} entfernen`}
              >
                ×
              </button>
            </span>
          ))}
          <button
            type="button"
            onClick={onClearAll}
            className="px-1 text-xs font-semibold text-muted-foreground hover:text-foreground"
          >
            Alle löschen
          </button>
        </div>
      )}
    </div>
  );
}
