import type { ReactNode } from "react";
import { cn } from "@/lib/utils";

/**
 * The pinned action bar at the bottom of every form: cancel, save and whatever else acts on the
 * entry. It never scrolls — it is a `shrink-0` sibling placed *after* the single `flex-1
 * overflow-y-auto` region inside a `flex flex-col overflow-hidden` column (see EditPageShell and
 * MassUpdateForm), so the content above it scrolls while the buttons stay in view. No
 * `position: sticky`/`fixed` is involved.
 *
 * The canonical button order lives in the callers: cancel leftmost, then the primary/submit action,
 * with destructive actions pushed right by a `flex-1` spacer. Cancel is always left of the primary
 * button (see projectforge-next/CLAUDE.md, "Form action buttons").
 */
export function FormActionBar({
  children,
  className,
}: {
  children: ReactNode;
  /** Inner wrapper classes, e.g. `mx-auto w-full max-w-3xl` to line the buttons up under a centered form. */
  className?: string;
}) {
  return (
    <div className="flex shrink-0 items-center border-t border-border bg-background px-6 py-2.5 shadow-[0_-2px_12px_rgba(0,0,0,0.05)]">
      <div className={cn("flex flex-1 items-center gap-3", className)}>
        {children}
      </div>
    </div>
  );
}
