"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { HugeiconsIcon } from "@hugeicons/react";
import { ArrowDown01Icon } from "@hugeicons/core-free-icons";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import { Button } from "@/components/ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
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
 *
 * On a narrow screen the row would wrap into a tall column next to the page's other actions, so there it is a
 * single menu button showing the current entry instead (CSS only, both are rendered).
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
  const current = items.find((item) => isActive(item, pathname));
  return (
    <>
      <nav aria-label={label} className="sm:hidden">
        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <Button variant="outline" size="sm">
              {current?.label ?? label}
              <HugeiconsIcon icon={ArrowDown01Icon} size={14} />
            </Button>
          </DropdownMenuTrigger>
          <DropdownMenuContent align="end">
            {items.map((item) => (
              <DropdownMenuItem key={item.href} asChild>
                <Link
                  href={item.href}
                  aria-current={item === current ? "page" : undefined}
                  className={cn(item === current && "font-semibold")}
                >
                  {item.label}
                </Link>
              </DropdownMenuItem>
            ))}
          </DropdownMenuContent>
        </DropdownMenu>
      </nav>
      <SectionNavRow items={items} label={label} pathname={pathname} />
    </>
  );
}

function SectionNavRow({
  items,
  label,
  pathname,
}: {
  items: SectionNavItem[];
  label: string;
  pathname: string;
}) {
  return (
    <nav
      aria-label={label}
      className="hidden flex-wrap items-center gap-2 sm:flex"
    >
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
