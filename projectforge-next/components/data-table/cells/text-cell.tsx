import { formatDisplayName, formatValue } from "@/lib/format";
import { getByPath } from "@/lib/dynamic/path";
import { cn } from "@/lib/utils";
import { HighlightedText } from "@/components/shared/highlighted-text";
import { isRefArray, RefList } from "@/components/shared/ref-list";
import { inactiveRefClass, type InactiveFlags } from "@/lib/page-def/reference";
import type { CellRenderProps } from "./cell-types";

/**
 * The terminal case: the value as localised text. Every formatter that produces
 * a string ends up here; the icon/bar formatters have their own components.
 */
export function TextCell({
  spec,
  value,
  row,
  ctx,
  highlight,
}: CellRenderProps) {
  const text = formatValue(value, spec.format, ctx);
  if (!text) return null;
  // A referenced object (or several) the server column formats by name reads as stale when it is deleted or
  // deactivated, as in the declared lists (see inactiveRefClass).
  // Only where the cell shows just their names, not a text the formatter built otherwise.
  const refs =
    isRefArray(value) && formatDisplayName(value) === text ? value : undefined;
  const refClass =
    !refs && typeof value === "object" && value !== null
      ? inactiveRefClass(value as InactiveFlags)
      : undefined;
  const tooltip = spec.tooltipPath
    ? getByPath(row, spec.tooltipPath)
    : undefined;
  return (
    <span
      // Right-aligned columns are the numeric ones (amounts, counts): monospaced digits so they line up.
      className={cn(
        "block truncate",
        spec.align === "right" && "text-right tabular-nums",
        refClass
      )}
      // Shown by the table's one delegated tooltip, see useOverflowTooltip.
      data-tooltip={
        typeof tooltip === "string" && tooltip ? tooltip : undefined
      }
    >
      {refs ? (
        <RefList refs={refs} highlight={highlight} />
      ) : (
        <HighlightedText text={text} query={highlight} />
      )}
    </span>
  );
}
