import { cn } from "@/lib/utils";

/**
 * Indeterminate loading indicator. Purely decorative — the surrounding element
 * carries the accessible name (e.g. `aria-busy` plus a visible label).
 *
 * Size comes from the caller so the same mark serves a full-page wait and an
 * inline one: pass e.g. `className="h-4 w-4 border-2"`.
 */
export function Spinner({ className }: { className?: string }) {
  return (
    <div
      aria-hidden
      className={cn(
        "h-8 w-8 animate-spin rounded-full border-4 border-muted border-t-primary",
        className
      )}
    />
  );
}
