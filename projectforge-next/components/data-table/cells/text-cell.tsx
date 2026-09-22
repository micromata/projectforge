import { formatValue } from "@/lib/format";
import { getByPath } from "@/lib/dynamic/path";
import { cn } from "@/lib/utils";
import { HighlightedText } from "@/components/shared/highlighted-text";
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
  const tooltip = spec.tooltipPath
    ? getByPath(row, spec.tooltipPath)
    : undefined;
  return (
    <span
      className={cn("block truncate", spec.align === "right" && "text-right")}
      // Shown by the table's one delegated tooltip, see useOverflowTooltip.
      data-tooltip={
        typeof tooltip === "string" && tooltip ? tooltip : undefined
      }
    >
      <HighlightedText text={text} query={highlight} />
    </span>
  );
}
