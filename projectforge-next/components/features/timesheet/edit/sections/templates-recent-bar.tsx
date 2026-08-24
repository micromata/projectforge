"use client";

import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { Clock01Icon } from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import { FavoritesMenu } from "@/components/shared/favorites/favorites-menu";
import { cn } from "@/lib/utils";
import type { TimesheetDetail } from "../../types";
import { useTimesheetTemplates } from "../use-timesheet-templates";

/**
 * The two ways of not typing a sheet from scratch: the user's last entries, applied with one click, and
 * their saved templates — the `timesheet.recent` and `timesheet.favorites` blocks of the legacy form in
 * one bar above the fields.
 *
 * Both fill the *what* of the sheet and leave its *when* (see useTimesheetTemplates): a recent entry is
 * the same work booked again, a template a shape the user named for it. Saving a template stores the
 * sheet on screen under a name; there is no "current" template to overwrite, so the menu offers create,
 * rename and delete but no in-place update.
 */
export function TemplatesRecentBar() {
  const t = useTranslations();
  const {
    recent,
    favorites,
    applyRecent,
    applyFavorite,
    create,
    rename,
    remove,
  } = useTimesheetTemplates();
  const entries = recent?.timesheets ?? [];

  return (
    <div className="flex items-center gap-2 rounded-md border border-border bg-muted/40 p-2">
      <FavoritesMenu
        favorites={favorites}
        label={t("timesheet.templates")}
        showLabel
        className="h-7 shrink-0"
        onSelect={applyFavorite}
        onCreate={create}
        onRename={rename}
        onDelete={remove}
      />
      {entries.length > 0 && (
        <>
          <span className="flex shrink-0 items-center gap-1 text-xs font-medium text-muted-foreground">
            <HugeiconsIcon icon={Clock01Icon} size={13} aria-hidden />
            {t("timesheet.recent")}
          </span>
          {/* One scrollable row rather than wrapping: the bar sits above the form (see editBanner) and
              a wrapping list would push the fields down as it grows. */}
          <div className="flex min-w-0 flex-1 gap-1 overflow-x-auto">
            {entries.map((entry) => (
              <RecentChip
                key={entry.counter ?? recentLabel(entry)}
                entry={entry}
                cost2Visible={recent?.cost2Visible ?? false}
                onClick={() => applyRecent(entry)}
              />
            ))}
          </div>
        </>
      )}
    </div>
  );
}

/** One recent entry as a button: its task, then whatever else tells two of the same task apart. */
function RecentChip({
  entry,
  cost2Visible,
  onClick,
}: {
  entry: TimesheetDetail;
  cost2Visible: boolean;
  onClick: () => void;
}) {
  return (
    <Button
      type="button"
      variant="outline"
      size="sm"
      onClick={onClick}
      className={cn(
        "h-7 max-w-56 shrink-0 justify-start gap-1.5 truncate text-xs"
      )}
    >
      <span className="truncate">{recentLabel(entry, cost2Visible)}</span>
    </Button>
  );
}

/**
 * A recent entry in one line: the task, its cost unit where the installation shows them, and the first
 * of location/reference/description that is set — enough to tell two bookings of the same task apart.
 */
function recentLabel(entry: TimesheetDetail, cost2Visible = false): string {
  const parts = [
    entry.task?.displayName,
    cost2Visible ? entry.kost2?.displayName : undefined,
    entry.location || entry.reference || entry.description,
  ];
  return parts.filter(Boolean).join(" · ") || "—";
}
