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
import { cn } from "@/lib/utils";
import { TaskConsumptionBar } from "@/components/shared/tasks/task-consumption-bar";
import type { TimesheetDetail } from "../../types";
import {
  RecentRow,
  consumptionTitle,
  searchText,
  taskName,
  type RecentColumn,
} from "./recent-timesheets-row";

export interface RecentTimesheetsPopoverProps {
  entries: TimesheetDetail[];
  /** Whether this installation books on cost units — the kost2 column is only worth showing then. */
  cost2Visible: boolean;
  /** A recent entry was picked; the caller merges its fields into the sheet on screen (see applyRecent). */
  onSelect: (entry: TimesheetDetail) => void;
  /**
   * Narrower, to stay within a dialog: opened from inside one (the multi-day booking), the box ends at
   * the dialog's edge instead of reaching over it.
   */
  compact?: boolean;
}

/**
 * The user's last bookings as a table to pick from, opened from the "Zuletzt verwendet" button — the
 * searchable recent list of the legacy form (`TimesheetTemplatesAndRecent`), which a scrolling row of
 * chips had flattened to one truncated line each. A popover, not an inline unfold: the bar sits above
 * the fields in a height-bounded modal, where a table opening in place would push them off screen.
 *
 * Each entry shows the consumption of its budget (the task's, or the first ancestor's with one), so a
 * reader booking time and material positions sees at a glance which one still has room.
 *
 * Kunde and Projekt sit beside the cost unit, on the same `cost2Visible` gate as the legacy table:
 * `getRecentList` resolves both from the entry's cost unit, so a reader who books on cost units gets
 * the same customer/project context they had in the classic form.
 */
export function RecentTimesheetsPopover({
  entries,
  cost2Visible,
  onSelect,
  compact = false,
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

  // The columns, in reading order — cost unit, its customer and project (all on the same `cost2Visible`
  // gate as the legacy table, since all three only exist for a cost-unit booking), then task, location,
  // description, tag and reference. `width` is a share of the fixed-layout table, so every cell truncates
  // to its column and the table never grows past the popover (no horizontal scroll, whatever the values).
  const columns: RecentColumn[] = [
    cost2Visible && {
      head: t("fibu.kost2._"),
      width: "w-[11%]",
      cell: (e: TimesheetDetail) => e.kost2?.displayName ?? "",
    },
    cost2Visible && {
      head: t("fibu.kunde._"),
      width: "w-[12%]",
      cell: (e: TimesheetDetail) => e.kost2?.project?.customer?.name ?? "",
    },
    cost2Visible && {
      head: t("fibu.projekt._"),
      width: "w-[12%]",
      cell: (e: TimesheetDetail) => e.kost2?.project?.name ?? "",
    },
    { head: t("task._"), width: "w-[15%]", cell: taskName },
    {
      // The budget's bar, as the form shows it once the entry is picked; not a link here, the row picks.
      head: t("task.consumption"),
      width: "w-[8%]",
      cell: consumptionTitle,
      render: (e: TimesheetDetail) =>
        e.task ? (
          <TaskConsumptionBar
            task={{ consumption: e.task.consumption, title: taskName(e) }}
            linkEnabled={false}
          />
        ) : null,
    },
    {
      head: t("timesheet.location"),
      width: "w-[10%]",
      cell: (e: TimesheetDetail) => e.location ?? "",
    },
    {
      head: t("description"),
      width: "w-[13%]",
      cell: (e: TimesheetDetail) => e.description ?? "",
    },
    {
      head: t("timesheet.tag"),
      width: "w-[9%]",
      cell: (e: TimesheetDetail) => e.tag ?? "",
    },
    {
      head: t("timesheet.reference"),
      width: "w-[10%]",
      cell: (e: TimesheetDetail) => e.reference ?? "",
    },
  ].filter(Boolean) as RecentColumn[];

  return (
    // `modal`, because this popover is opened from inside the edit dialog: a non-modal layer stays
    // outside the dialog's scroll lock (react-remove-scroll), so the wheel never reaches the table
    // below. A modal layer wraps its own content as the active scroll region — the same reason a
    // Select scrolls inside a dialog and a plain popover does not.
    <Popover open={open} onOpenChange={setOpen} modal>
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
        className={cn(
          "flex max-h-[min(28rem,70vh)] w-[92vw] flex-col gap-0 overflow-hidden p-0",
          compact ? "sm:w-[min(46rem,85vw)]" : "sm:w-[min(72rem,92vw)]"
        )}
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
          {/* `table-fixed` and the smaller type let all columns show at once; each cell truncates to
              its share above, so a long structure element or reference no longer widens the table. */}
          <Table className="table-fixed text-[11px]">
            <TableHeader>
              <TableRow>
                {columns.map((col) => (
                  <TableHead
                    key={col.head}
                    className={cn(col.width, "h-8 px-2 py-1")}
                  >
                    {col.head}
                  </TableHead>
                ))}
              </TableRow>
            </TableHeader>
            <TableBody>
              {filtered.length === 0 ? (
                <TableRow>
                  <TableCell
                    colSpan={columns.length}
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
                    columns={columns}
                    query={query}
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
