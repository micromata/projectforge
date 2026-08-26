"use client";

import { useMemo, useState } from "react";
import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { Clock01Icon, Search01Icon } from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from "@/components/ui/popover";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import type { TimesheetDetail } from "../../types";

export interface RecentTimesheetsPopoverProps {
  entries: TimesheetDetail[];
  /** Whether this installation books on cost units — the kost2 column is only worth showing then. */
  cost2Visible: boolean;
  /** A recent entry was picked; the caller merges its fields into the sheet on screen (see applyRecent). */
  onSelect: (entry: TimesheetDetail) => void;
}

/**
 * The user's last bookings as a table to pick from, opened from the "Zuletzt verwendet" button — the
 * searchable recent list of the legacy form (`TimesheetTemplatesAndRecent`), which a scrolling row of
 * chips had flattened to one truncated line each. A popover, not an inline unfold: the bar sits above
 * the fields in a height-bounded modal, where a table opening in place would push them off screen.
 *
 * Kunde/Projekt of the legacy table are dropped — the cost unit reference carries neither here.
 */
export function RecentTimesheetsPopover({
  entries,
  cost2Visible,
  onSelect,
}: RecentTimesheetsPopoverProps) {
  const t = useTranslations();
  const [open, setOpen] = useState(false);
  const [query, setQuery] = useState("");

  // Filtered over the same fields the row shows, so what a reader types matches what they read.
  const filtered = useMemo(() => {
    const needle = query.trim().toLowerCase();
    if (!needle) return entries;
    return entries.filter((entry) => searchText(entry).includes(needle));
  }, [entries, query]);

  return (
    <Popover open={open} onOpenChange={setOpen}>
      <PopoverTrigger asChild>
        <Button
          type="button"
          variant="outline"
          size="sm"
          aria-label={t("timesheet.recent")}
          className="h-7 shrink-0 gap-1.5"
        >
          <HugeiconsIcon icon={Clock01Icon} size={14} aria-hidden />
          <span className="truncate">{t("timesheet.recent")}</span>
        </Button>
      </PopoverTrigger>
      <PopoverContent
        align="start"
        // A fixed, viewport-bounded box with overflow-hidden so the table can't push it wider than
        // the screen; the scroll region below then scrolls both ways within it.
        className="flex max-h-[min(28rem,70vh)] w-[92vw] flex-col gap-0 overflow-hidden p-0 sm:w-[42rem]"
      >
        <div className="flex shrink-0 items-center gap-2 border-b px-3 py-2">
          <HugeiconsIcon
            icon={Search01Icon}
            size={14}
            aria-hidden
            className="shrink-0 text-muted-foreground"
          />
          <Input
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder={t("search._")}
            aria-label={t("search._")}
            className="h-7 border-0 p-0 shadow-none focus-visible:ring-0"
          />
        </div>
        <div className="min-h-0 flex-1 overflow-auto">
          <Table>
            <TableHeader>
              <TableRow>
                {cost2Visible && <TableHead>{t("fibu.kost2._")}</TableHead>}
                <TableHead>{t("task._")}</TableHead>
                <TableHead>{t("timesheet.location")}</TableHead>
                <TableHead>{t("timesheet.tag")}</TableHead>
                <TableHead>{t("timesheet.reference")}</TableHead>
                <TableHead>{t("description")}</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {filtered.length === 0 ? (
                <TableRow>
                  <TableCell
                    colSpan={cost2Visible ? 6 : 5}
                    className="py-6 text-center text-muted-foreground"
                  >
                    {t("nothingFound")}
                  </TableCell>
                </TableRow>
              ) : (
                filtered.map((entry) => (
                  <RecentRow
                    key={entry.counter ?? searchText(entry)}
                    entry={entry}
                    cost2Visible={cost2Visible}
                    onSelect={() => {
                      onSelect(entry);
                      setOpen(false);
                    }}
                  />
                ))
              )}
            </TableBody>
          </Table>
        </div>
      </PopoverContent>
    </Popover>
  );
}

/** One recent entry as a clickable row — its columns match the header, gated on `cost2Visible`. */
function RecentRow({
  entry,
  cost2Visible,
  onSelect,
}: {
  entry: TimesheetDetail;
  cost2Visible: boolean;
  onSelect: () => void;
}) {
  return (
    <TableRow
      role="button"
      tabIndex={0}
      onClick={onSelect}
      onKeyDown={(e) => {
        if (e.key === "Enter" || e.key === " ") {
          e.preventDefault();
          onSelect();
        }
      }}
      className="cursor-pointer"
    >
      {cost2Visible && <TableCell>{entry.kost2?.displayName ?? ""}</TableCell>}
      <TableCell>{entry.task?.displayName ?? ""}</TableCell>
      <TableCell>{entry.location ?? ""}</TableCell>
      <TableCell>{entry.tag ?? ""}</TableCell>
      <TableCell>{entry.reference ?? ""}</TableCell>
      <TableCell className="max-w-64 truncate">
        {entry.description ?? ""}
      </TableCell>
    </TableRow>
  );
}

/** The one lower-cased string a row is matched against — every field it shows (see filterRecent, legacy). */
function searchText(entry: TimesheetDetail): string {
  return [
    entry.task?.displayName,
    entry.kost2?.displayName,
    entry.location,
    entry.tag,
    entry.reference,
    entry.description,
  ]
    .filter(Boolean)
    .join(" ")
    .toLowerCase();
}
