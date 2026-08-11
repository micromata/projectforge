import { cn } from "@/lib/utils";
import type { CellRenderProps } from "./cell-types";

/**
 * A task's status, coloured as the Wicket page shows it (`taskStatus_opened` /
 * `taskStatus_closed`).
 *
 * The colour follows the row's raw `status` — the enum letter — rather than the
 * text, so it survives translation. The text itself is the backend's
 * `statusAsString`, which already reads "gelöscht" for a deleted task.
 */
const STATUS_CLASSES: Record<string, string> = {
  O: "task-status-opened",
  C: "task-status-closed",
  // "N" (not opened) keeps the row's own colour, as in Wicket.
};

export function TaskStatusCell({ value, row }: CellRenderProps) {
  if (typeof value !== "string" || !value) return null;
  const deleted = row.deleted === true;
  const status = typeof row.status === "string" ? row.status : undefined;
  return (
    <span
      className={cn(
        "max-w-full truncate",
        // A deleted task's status is dimmed rather than coloured, and not struck
        // through: the word "gelöscht" is the row's explanation, not its content.
        // `inline-block` is what keeps it unstruck — the row's line-through comes
        // from the cell and propagates into every box but an atomic inline one, so
        // no descendant declaration can cancel it.
        deleted
          ? "inline-block text-muted-foreground"
          : cn("block", status && STATUS_CLASSES[status])
      )}
    >
      {value}
    </span>
  );
}
