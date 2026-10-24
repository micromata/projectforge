"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { Clock01Icon } from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from "@/components/ui/popover";
import { cn } from "@/lib/utils";
import type { TimesheetDetail } from "../../types";
import { RecentTimesheetsTable } from "./recent-timesheets-table";

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
 *
 * The table is the shared grid: sortable and filterable per column, its columns reorderable, resizable,
 * pinnable and hideable. The layout and the sort order are stored in the user's prefs; the column filters
 * are not, like the search they are gone once the box closes (see useTableState).
 */
export function RecentTimesheetsPopover({
  entries,
  cost2Visible,
  onSelect,
  compact = false,
}: RecentTimesheetsPopoverProps) {
  const t = useTranslations();
  const [open, setOpen] = useState(false);

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
        // the screen; the table's own scroller below then scrolls both ways within it.
        className={cn(
          "flex max-h-[min(28rem,70vh)] w-[92vw] flex-col gap-0 overflow-hidden p-0",
          compact ? "sm:w-[min(46rem,85vw)]" : "sm:w-[min(72rem,92vw)]"
        )}
      >
        {/* Mounted while open only: the stored layout is read on the first opening, cached afterwards. */}
        <RecentTimesheetsTable
          entries={entries}
          cost2Visible={cost2Visible}
          onSelect={(entry) => {
            onSelect(entry);
            setOpen(false);
          }}
        />
      </PopoverContent>
    </Popover>
  );
}
