"use client";

import { useTranslations } from "next-intl";
import { FavoritesMenu } from "@/components/shared/favorites/favorites-menu";
import { useTimesheetTemplates } from "../use-timesheet-templates";
import { RecentTimesheetsPopover } from "./recent-timesheets-popover";

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
        modal
        className="h-7 shrink-0"
        onSelect={applyFavorite}
        onCreate={create}
        onRename={rename}
        onDelete={remove}
      />
      {entries.length > 0 && (
        <RecentTimesheetsPopover
          entries={entries}
          cost2Visible={recent?.cost2Visible ?? false}
          onSelect={applyRecent}
        />
      )}
    </div>
  );
}
