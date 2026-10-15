import { GuardedLink } from "@/components/shared/guarded-link";
import { resolveMenuUrl } from "@/lib/menu-url";

/**
 * Hover feedback for menu entries and triggers. The shadcn menu primitives only highlight on
 * `focus:` and set `cursor-default`, which reads as inactive for entries that are in fact links.
 */
export const MENU_HOVER_CLASS =
  "cursor-pointer hover:bg-accent hover:text-accent-foreground";

/**
 * Renders a menu entry as a client-side link when it belongs to this app, and as a plain anchor
 * (full page load) when it points at the legacy React app or Wicket.
 *
 * Deliberately free of state and side effects, so it stays usable from a Server Component and costs
 * nothing per instance — it is rendered per table row in places like OrdersCell. Reporting an opened
 * menu entry is therefore the caller's job (useReportMenuUsage), not this component's.
 *
 * The internal case is a [GuardedLink]: every menu entry leads away from whatever is on screen, and an
 * edit form full of entries is one of the things it leads away from. The external case needs no guard
 * of its own — a full page load is what `beforeunload` catches (see useUnsavedChangesWarning).
 */
export function MenuLink({
  url,
  children,
  className,
  onClick,
  "aria-label": ariaLabel,
}: {
  url: string | undefined;
  children: React.ReactNode;
  className?: string;
  /** The event is passed on, so a link inside a clickable row can stop propagation. */
  onClick?: (event: React.MouseEvent) => void;
  /** For a link whose content carries no text of its own, e.g. the bar of a ConsumptionCell. */
  "aria-label"?: string;
}) {
  const target = resolveMenuUrl(url);
  if (target.kind === "external") {
    return (
      <a
        href={target.href}
        className={className}
        onClick={onClick}
        aria-label={ariaLabel}
      >
        {children}
      </a>
    );
  }
  return (
    <GuardedLink
      href={target.href}
      className={className}
      onClick={onClick}
      aria-label={ariaLabel}
    >
      {children}
    </GuardedLink>
  );
}
