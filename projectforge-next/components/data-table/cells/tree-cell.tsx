import { HugeiconsIcon } from "@hugeicons/react";
import { ArrowDown01Icon, ArrowRight01Icon } from "@hugeicons/core-free-icons";
import { getByPath } from "@/lib/dynamic/path";
import type { CellRenderProps } from "./cell-types";

/** TaskNode.treeStatus as the backend sends it alongside the row's `indent`. */
type TreeStatus = "OPENED" | "CLOSED" | "LEAF";

/**
 * A tree node's title, indented by its depth and preceded by a chevron.
 *
 * Expanding/collapsing is not wired up yet — that needs the task tree endpoints
 * and an expansion model in DataTable — so the chevron shows the server's state
 * without being clickable. `onToggle` is already in the signature so the
 * interactive version doesn't have to change the call sites.
 */
export function TreeCell({
  spec,
  value,
  row,
  t,
  onToggle,
}: CellRenderProps & { onToggle?: () => void }) {
  const declared = spec.tooltipPath
    ? getByPath(row, spec.tooltipPath)
    : undefined;
  const tooltip =
    typeof declared === "string" && declared ? declared : undefined;
  const status = (row.treeStatus as TreeStatus | undefined) ?? "LEAF";
  const indent = typeof row.indent === "number" ? row.indent : 0;
  const icon =
    status === "OPENED"
      ? ArrowDown01Icon
      : status === "CLOSED"
        ? ArrowRight01Icon
        : undefined;
  return (
    <span
      className="flex items-center gap-1 truncate"
      style={{ paddingLeft: `${indent * 0.9}rem` }}
    >
      <span className="inline-flex size-4 shrink-0 items-center justify-center text-muted-foreground">
        {icon ? (
          <HugeiconsIcon
            icon={icon}
            size={13}
            role="img"
            aria-label={t(status === "OPENED" ? "collapse" : "expand")}
            onClick={onToggle}
          />
        ) : null}
      </span>
      {/* The declared tooltip where the column has one (the task's description), otherwise none: the
          delegated tooltip shows the title itself as soon as the cell clips it. */}
      <span className="truncate" data-tooltip={tooltip}>
        {String(value ?? "")}
      </span>
    </span>
  );
}
