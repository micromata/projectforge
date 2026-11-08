import type { ReactNode } from "react";
import { Card } from "@/components/ui/card";
import { cn } from "@/lib/utils";

/**
 * A key figure as a small tile: the label in capitals at the top, the value large and right aligned, and
 * optionally a note at the bottom left and one at the bottom right (e.g. the last day the value covers).
 * With [onClick] the tile is a toggle button (e.g. a filter), [active] marking it as pressed.
 */
export function KpiTile({
  label,
  note,
  noteRight,
  compact = false,
  onClick,
  active = false,
  disabled = false,
  className,
  children,
}: {
  label: string;
  note?: string;
  noteRight?: string;
  /** Less padding and a smaller value, for a row of tiles above a table. */
  compact?: boolean;
  onClick?: () => void;
  active?: boolean;
  disabled?: boolean;
  className?: string;
  children: ReactNode;
}) {
  const tile = (
    <Card
      size="sm"
      className={cn(
        "border px-4 shadow-sm",
        compact ? "h-full gap-0.5 py-2" : "gap-1 py-3",
        onClick && !disabled && "transition-colors hover:bg-muted",
        active && "ring-2 ring-primary",
        disabled && "opacity-60",
        className
      )}
    >
      <span className="text-[11.5px] font-semibold uppercase tracking-wide text-muted-foreground">
        {label}
      </span>
      <span
        className={cn(
          "text-right font-semibold tabular-nums",
          compact ? "text-lg leading-tight" : "text-xl"
        )}
      >
        {children}
      </span>
      {(note || noteRight) && (
        <span className="mt-auto flex justify-between gap-2 text-xs text-muted-foreground">
          <span>{note}</span>
          <span className="tabular-nums">{noteRight}</span>
        </span>
      )}
    </Card>
  );
  if (!onClick) return tile;
  return (
    <button
      type="button"
      className="rounded-lg text-left focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring disabled:cursor-default"
      aria-pressed={active}
      disabled={disabled}
      onClick={onClick}
    >
      {tile}
    </button>
  );
}
