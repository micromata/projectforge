"use client";

import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { MoreHorizontalIcon } from "@hugeicons/core-free-icons";
import { useOverflowCount } from "@/hooks/use-overflow-count";
import type { MenuItem } from "@/lib/rs/types";
import { Button } from "@/components/ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { MenuLink } from "@/components/shared/menu-link";

/**
 * The user's favourite menu entries, shown next to the main menu. Entries that no longer fit into
 * the remaining nav width move into an overflow menu instead of pushing into the user menu.
 */
export function FavoritesBar({ items }: { items: MenuItem[] }) {
  const t = useTranslations("menu");
  const { containerRef, measureRef, visibleCount } = useOverflowCount({
    itemCount: items.length,
  });

  if (items.length === 0) return null;

  const visible = items.slice(0, visibleCount);
  const hidden = items.slice(visibleCount);

  return (
    <div
      ref={containerRef}
      className="relative flex min-w-0 flex-1 items-center gap-1 overflow-hidden"
    >
      {/* Off-screen reference row: keeps every entry's width known while it sits in the overflow. */}
      <div
        ref={measureRef}
        aria-hidden
        className="pointer-events-none invisible absolute flex items-center gap-1"
      >
        {items.map((item) => (
          <FavoriteButton key={itemKey(item)} item={item} />
        ))}
      </div>
      {visible.map((item) => (
        <FavoriteButton key={itemKey(item)} item={item} />
      ))}
      {hidden.length > 0 ? (
        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <Button
              variant="ghost"
              size="sm"
              className="shrink-0"
              aria-label={t("favorites.more")}
            >
              <HugeiconsIcon icon={MoreHorizontalIcon} size={16} />
            </Button>
          </DropdownMenuTrigger>
          <DropdownMenuContent align="start">
            {hidden.map((item) => (
              <DropdownMenuItem key={itemKey(item)} asChild>
                <MenuLink url={item.url}>{item.title}</MenuLink>
              </DropdownMenuItem>
            ))}
          </DropdownMenuContent>
        </DropdownMenu>
      ) : null}
    </div>
  );
}

function FavoriteButton({ item }: { item: MenuItem }) {
  return (
    <Button variant="ghost" size="sm" className="shrink-0" asChild>
      <MenuLink url={item.url}>{item.title}</MenuLink>
    </Button>
  );
}

function itemKey(item: MenuItem): string {
  return item.key ?? item.url ?? item.title;
}
