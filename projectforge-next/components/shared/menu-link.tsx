import Link from "next/link";
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
 */
export function MenuLink({
  url,
  children,
  className,
  onClick,
}: {
  url: string | undefined;
  children: React.ReactNode;
  className?: string;
  /** The event is passed on, so a link inside a clickable row can stop propagation. */
  onClick?: (event: React.MouseEvent) => void;
}) {
  const target = resolveMenuUrl(url);
  if (target.kind === "external") {
    return (
      <a href={target.href} className={className} onClick={onClick}>
        {children}
      </a>
    );
  }
  return (
    <Link href={target.href} className={className} onClick={onClick}>
      {children}
    </Link>
  );
}
