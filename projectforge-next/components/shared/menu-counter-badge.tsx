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
 */
export function MenuCounterBadge({ badge }: { badge?: MenuBadge }) {
  if (!badge?.counter) return null;
  return (
    <span
      title={badge.tooltip ?? undefined}
      className={cn(
        "ml-auto inline-flex h-5 min-w-5 shrink-0 items-center justify-center rounded-full px-1 text-xs",
        // The backend flags urgent counts (open items) as "danger"; everything else is neutral.
        badge.style === "danger"
          ? "bg-destructive text-white"
          : "bg-primary text-primary-foreground"
      )}
    >
      {badge.counter}
    </span>
  );
}
