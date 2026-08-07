import { cn } from "@/lib/utils";
import type { CellRenderProps } from "./cell-types";

/** The Consumption DTO the backend puts into the cell (rest/task/Consumption.kt). */
interface Consumption {
  /** Pre-rendered, already localised, e.g. "350PT/188PT (186%)". */
  title?: string;
  /** One of the "progress-*" names of Consumption.Status. */
  status?: string;
  barPercentage?: number;
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
 */
export function ConsumptionCell({ value, t }: CellRenderProps) {
  if (!value || typeof value !== "object") return null;
  const { title, status, barPercentage } = value as Consumption;
  const percentage = Math.min(Math.max(barPercentage ?? 0, 0), 100);
  return (
    <span
      className={cn(
        "consumption-track",
        STATUS_CLASS[status ?? ""] ?? "consumption-none"
      )}
      role="img"
      aria-label={
        title ? `${t("task.consumption")}: ${title}` : t("task.consumption")
      }
      title={title}
    >
      <span className="consumption-bar" style={{ width: `${percentage}%` }} />
    </span>
  );
}
