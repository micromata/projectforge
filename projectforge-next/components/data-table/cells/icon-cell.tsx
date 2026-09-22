import { HugeiconsIcon } from "@hugeicons/react";
import { cellIcon } from "./cell-icons";
import type { CellRenderProps } from "./cell-types";

/**
 * An icon looked up by the cell's value (a column def's `valueIconMap`, e.g.
 * `{true: STAR_REGULAR}` on the address list's favourite column). A value the map
 * doesn't cover renders nothing — that is the map's way of saying "no icon".
 */
export function IconCell({ spec, value }: CellRenderProps) {
  const key = value === null || value === undefined ? "" : String(value);
  const icon = cellIcon(spec.valueIcons?.[key]);
  if (!icon) return null;
  return (
    <HugeiconsIcon
      icon={icon}
      size={14}
      className="text-muted-foreground"
      aria-hidden
    />
  );
}
