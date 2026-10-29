"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";

export interface SectionNavItem {
  href: string;
  label: string;
  /**
   * Path prefixes that make the entry the current one as well, for the pages below an area that are not
   * under its own `href` (a detail page of the list the entry leads to).
   */
  match?: string[];
  /** Active on `href` itself only, not on the paths below it — for an entry whose `href` is a prefix of the others. */
  exact?: boolean;
  /** Explains the entry on hover. */
  tooltip?: string;
  className?: string;
}

function isActive(item: SectionNavItem, pathname: string): boolean {
  const under = (prefix: string) =>
    pathname === prefix || pathname.startsWith(`${prefix}/`);
  return (
    (item.exact ? pathname === item.href : under(item.href)) ||
    (item.match ?? []).some(under)
  );
}

/**
 * The small menu of an area (a plugin) at the top right of its pages: one button per page of the area,
 * the current one highlighted. Knows nothing of the area — the caller decides which entries the user gets.
 */
export function SectionNav({
  items,
  label,
}: {
  items: SectionNavItem[];
  /** Names the navigation for assistive technology, e.g. the name of the area. */
  label: string;
}) {
  const pathname = usePathname();
  if (items.length === 0) return null;
  return (
    <nav aria-label={label} className="flex flex-wrap items-center gap-2">
      {items.map((item) => {
        const active = isActive(item, pathname);
        return (
          <HintTooltip key={item.href} text={item.tooltip}>
            <Button
              asChild
              variant={active ? "default" : "outline"}
              size="sm"
              className={cn(!active && item.className)}
            >
              <Link href={item.href} aria-current={active ? "page" : undefined}>
                {item.label}
              </Link>
            </Button>
          </HintTooltip>
        );
      })}
    </nav>
  );
}
