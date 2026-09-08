import { MenuLink } from "@/components/shared/menu-link";
import { timesheetListHref } from "@/lib/timesheet-links";
import { cn } from "@/lib/utils";
import type { CellRenderProps } from "./cell-types";

/** The Consumption DTO the backend puts into the cell (rest/task/Consumption.kt). */
interface Consumption {
  /** Pre-rendered, already localised, e.g. "350PT/188PT (186%)". */
  title?: string;
  /** One of the "progress-*" names of Consumption.Status. */
  status?: string;
  barPercentage?: number;
  /** The task the effort was booked against — the bar's link target. */
  id?: number;
}

/** Consumption.Status → the token pair driving the bar's track and fill. */
const STATUS_CLASS: Record<string, string> = {
  "progress-none": "consumption-none",
  "progress-done": "consumption-done",
  "progress-80": "consumption-80",
  "progress-90": "consumption-90",
  "progress-overbooked": "consumption-overbooked",
  "progress-overbooked-min": "consumption-overbooked-min",
};

/**
 * A progress bar for a task's booked-versus-planned effort. The percentage and
 * the colour both come from the server (it knows the "finished" flag), so this
 * only paints. `title` is the tooltip the legacy app showed next to the bar.
 *
 * The bar is a link to the time sheets behind it, filtered to the task — what
 * Wicket's `ConsumptionBarPanel` does, and the only way from the tree to the
 * bookings the number is made of. Unless [CellRenderProps.linkEnabled] says
 * otherwise, as in a select popover.
 */
export function ConsumptionCell({
  value,
  row,
  t,
  linkEnabled = true,
}: CellRenderProps) {
  if (!value || typeof value !== "object") return null;
  const { title, status, barPercentage, id } = value as Consumption;
  // The task's name for the filter pill: the consumption value carries the id, but the row it sits on is
  // the task, so its title is the name to show. Both the task list row and the tree node carry `title`.
  const taskName = typeof row?.title === "string" ? row.title : undefined;
  const percentage = Math.min(Math.max(barPercentage ?? 0, 0), 100);
  const label = title
    ? `${t("task.consumption")}: ${title}`
    : t("task.consumption");
  // The tooltip is anchored to this element's rect (see useOverflowTooltip), so it must be the bar
  // itself and not a wrapper: the track is a `display:block` box of the bar's own width, while an inline
  // wrapper around a block child reports a rect as wide as the whole row and the tooltip drifts off it.
  const bar = (
    <span
      className={cn(
        "consumption-track",
        STATUS_CLASS[status ?? ""] ?? "consumption-none"
      )}
      data-tooltip={title}
    >
      <span className="consumption-bar" style={{ width: `${percentage}%` }} />
    </span>
  );

  // Shown by the table's one delegated tooltip, see useOverflowTooltip. `block` wrappers so the bar keeps
  // its own box; the tooltip is found by `closest` from the `data-tooltip` on the bar above.
  if (!linkEnabled || id == null) {
    return (
      <span role="img" aria-label={label} className="block">
        {bar}
      </span>
    );
  }
  return (
    <span className="block">
      <MenuLink
        // The time sheets of this task, on this app's list (the time sheets are migrated), filtered to
        // the task. The route reads the two params back and seeds a transient, cleared filter — the task
        // (with its name for the pill), not remembered afterwards, as Wicket's `ConsumptionBarPanel` set
        // `taskId`/`clear=true`/`storeFilter=false` (see app/(authenticated)/timesheet/page.tsx). An
        // internal `next/` url now, so the jump is a client-side navigation.
        url={timesheetListHref(id, taskName)}
        className="block"
        aria-label={`${t("timesheet.title.list")}: ${label}`}
        // The row itself is clickable (it selects the task), so a click on the bar must not count.
        onClick={(event) => event.stopPropagation()}
      >
        {bar}
      </MenuLink>
    </span>
  );
}
