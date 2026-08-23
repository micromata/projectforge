import { MenuLink } from "@/components/shared/menu-link";
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
  t,
  linkEnabled = true,
}: CellRenderProps) {
  if (!value || typeof value !== "object") return null;
  const { title, status, barPercentage, id } = value as Consumption;
  const percentage = Math.min(Math.max(barPercentage ?? 0, 0), 100);
  const label = title
    ? `${t("task.consumption")}: ${title}`
    : t("task.consumption");
  const bar = (
    <span
      className={cn(
        "consumption-track",
        STATUS_CLASS[status ?? ""] ?? "consumption-none"
      )}
    >
      <span className="consumption-bar" style={{ width: `${percentage}%` }} />
    </span>
  );

  // Shown by the table's one delegated tooltip, see useOverflowTooltip. On the wrapper rather than on
  // the link, as in OrdersCell: the tooltip is found by `closest` and MenuLink renders the anchor.
  if (!linkEnabled || id == null) {
    return (
      <span role="img" aria-label={label} data-tooltip={title}>
        {bar}
      </span>
    );
  }
  return (
    <span className="block" data-tooltip={title}>
      <MenuLink
        // The time sheets of this task, on Wicket's list and with its own three parameters
        // (ConsumptionBarPanel): the task, and a filter that is cleared for it and not remembered
        // afterwards. Wicket rather than the React app, whose list was never finished — see
        // MIGRATION.md, and TaskEditLink, which spells out a legacy url for the same reason.
        url={`wa/timesheetList?taskId=${id}&clear=true&storeFilter=false`}
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
