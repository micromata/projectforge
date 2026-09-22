import { cn } from "@/lib/utils";
import type { MenuBadge } from "@/lib/rs/types";

/**
 * The little counter pill a menu entry (or a whole category) carries when the backend reports open
 * items behind it — open leave applications, an unfinished 2FA setup, and so on. It mirrors the
 * legacy React `MenuBadge`: a single place every menu renders through, so the main menu, the user
 * menu and the favourites bar can never drift apart again.
 *
 * Returns null when there is nothing to show, so callers can drop it in unconditionally. The backend
 * accumulates child counters onto their parent category (MenuItem.postProcess), which is why this is
 * rendered on category headers as well as on leaves.
 *
 * Two shapes for two contexts (`variant`):
 * - `inline` (default): a right-aligned pill for the vertical dropdown panels, where a row has the
 *   width to carry it beside the label.
 * - `corner`: a smaller badge raised as a superscript, the way Wicket's horizontal menu bar shows it.
 *   Kept in the normal flow (not absolute) so it never clips against the bar's edge and never lands
 *   on a neighbouring dropdown caret; place it right after the label text (before any caret).
 */
export function MenuCounterBadge({
  badge,
  variant = "inline",
}: {
  badge?: MenuBadge;
  variant?: "inline" | "corner";
}) {
  if (!badge?.counter) return null;
  // Counters are always red: the backend hard-codes "danger" on the accumulated parent totals
  // (MenuItem.postProcess) while leaf badges arrive style-less, but a menu counter marks open items
  // either way, so it reads as the same alert everywhere the menu shows one.
  return (
    <span
      title={badge.tooltip ?? undefined}
      className={cn(
        "inline-flex shrink-0 items-center justify-center rounded-full bg-destructive text-white",
        variant === "corner"
          ? "relative -top-1.5 ml-0.5 h-3.5 min-w-3.5 px-1 text-[9px] leading-none"
          : "ml-auto h-5 min-w-5 px-1 text-xs"
      )}
    >
      {badge.counter}
    </span>
  );
}
