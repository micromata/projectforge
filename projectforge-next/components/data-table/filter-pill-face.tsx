import { cn } from "@/lib/utils";

/**
 * The frame of a filter pill: filled reads as solid, empty as a dashed outline. Shared by the pills of the
 * filter row ([FilterPillShell]) and a chart's summary of them ([AppliedFilterSummary]), so the list's
 * filter looks the same wherever it is shown.
 */
export function filterPillFrameClass(active: boolean): string {
  return cn(
    "inline-flex max-w-full items-center rounded-full border text-xs font-medium",
    active
      ? "border-primary/30 bg-primary/10 text-primary"
      : "border-dashed border-muted-foreground/40 text-muted-foreground"
  );
}

/**
 * What a filter pill reads: "Kunde: A, B, C +12". The text truncates, the count beside it does not — cut
 * off at the end of a long text, "+12" would be the first thing to go.
 */
export function FilterPillFace({
  label,
  text,
  more = 0,
  className,
}: {
  label: string;
  text?: string;
  /** Picks left off [text] (see [filterPillContent]). */
  more?: number;
  className?: string;
}) {
  return (
    <span className={cn("flex min-w-0 max-w-64 items-center gap-1", className)}>
      <span className="truncate">
        {label}
        {text && `: ${text}`}
      </span>
      {more > 0 && <span className="shrink-0">+{more}</span>}
    </span>
  );
}
