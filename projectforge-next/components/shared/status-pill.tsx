import type { CSSProperties } from "react";
import { cn } from "@/lib/utils";

/**
 * A traffic-light state, not a value: success is green, danger red, info the teal "not yet done" tint,
 * neutral the muted grey. The generic sibling of `book/status-badge.tsx` (which predates it and could
 * adopt it), lifted to `shared/` so any feature can badge a derived state without a cross-feature import.
 */
export type StatusTone = "success" | "danger" | "info" | "neutral";

/**
 * Colours come from CSS vars (the one inline-`style` exception the styling rules allow, for dynamic
 * colour), so a tone tints in both themes from a single token trio (`--status-*`).
 */
const TONE_STYLE: Record<StatusTone, CSSProperties> = {
  success: {
    background: "var(--status-available-bg)",
    color: "var(--status-available)",
    borderColor: "var(--status-available-border)",
  },
  danger: {
    background: "var(--status-loaned-bg)",
    color: "var(--status-loaned)",
    borderColor: "var(--status-loaned-border)",
  },
  info: {
    background: "var(--status-info-bg)",
    color: "var(--status-info)",
    borderColor: "var(--status-info-border)",
  },
  neutral: {
    background: "var(--muted)",
    color: "var(--muted-foreground)",
    borderColor: "var(--border)",
  },
};

/**
 * A pill saying what state something is in — a paid/overdue/open invoice, a lent-out book. The dot and
 * the fill share the tone's colour (`bg-current`), so the state reads at a glance and the label spells it out.
 */
export function StatusPill({
  tone,
  label,
  className,
}: {
  tone: StatusTone;
  label: string;
  className?: string;
}) {
  return (
    <span
      className={cn(
        "inline-flex shrink-0 items-center gap-1.5 whitespace-nowrap rounded-full border px-2.5 py-0.5 text-xs font-medium",
        className
      )}
      style={TONE_STYLE[tone]}
    >
      <span aria-hidden className="size-1.5 rounded-full bg-current" />
      {label}
    </span>
  );
}
