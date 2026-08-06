"use client";

import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import {
  ArrowDown01Icon,
  MoreHorizontalIcon,
} from "@hugeicons/core-free-icons";
import { useOverflowCount } from "@/hooks/use-overflow-count";
import { cn } from "@/lib/utils";
import type { MenuItem } from "@/lib/rs/types";
import { buttonVariants } from "@/components/ui/button";
import {
  MenubarContent,
  MenubarItem,
  MenubarMenu,
  MenubarSub,
  MenubarSubContent,
  MenubarSubTrigger,
  MenubarTrigger,
} from "@/components/ui/menubar";
import { MENU_HOVER_CLASS, MenuLink } from "@/components/shared/menu-link";

/** Shared by the real entries and the hidden measurement row, so both are exactly as wide. */
const ENTRY_CLASS = cn(
  buttonVariants({ variant: "ghost", size: "sm" }),
  "shrink-0 cursor-pointer"
);

/**
 * The user's favourite menu entries, shown next to the main menu. Entries that no longer fit into
 * the remaining nav width move into an overflow menu instead of pushing into the user menu.
 *
 * Must be rendered inside the nav's `Menubar`: a favourite can be a folder, and the menubar is what
 * lets one click switch from an open folder straight to another menu.
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
      {/*
       * Off-screen reference row: keeps every entry's width known while it sits in the overflow.
       * Plain markup rather than real triggers — duplicate menus would join the menubar's focus
       * order.
       */}
      <div
        ref={measureRef}
        aria-hidden
        className="pointer-events-none invisible absolute flex items-center gap-1"
      >
        {items.map((item) => (
          <span key={itemKey(item)} className={ENTRY_CLASS}>
            {item.title}
            {item.subMenu?.length ? (
              <HugeiconsIcon icon={ArrowDown01Icon} size={14} />
            ) : null}
          </span>
        ))}
      </div>
      {visible.map((item) => (
        <FavoriteEntry key={itemKey(item)} item={item} />
      ))}
      {hidden.length > 0 ? (
        <MenubarMenu>
          <MenubarTrigger
            className={cn(ENTRY_CLASS, "px-2")}
            aria-label={t("favorites.more")}
          >
            <HugeiconsIcon icon={MoreHorizontalIcon} size={16} />
          </MenubarTrigger>
          <MenubarContent align="start">
            {hidden.map((item) => (
              <MenuEntry key={itemKey(item)} item={item} />
            ))}
          </MenubarContent>
        </MenubarMenu>
      ) : null}
    </div>
  );
}

/**
 * A favourite is either a direct link or a folder the user created while customizing their menu
 * (see FavoritesMenuCreator: "Administration", "Projektmanagement", …), which opens its entries.
 */
function FavoriteEntry({ item }: { item: MenuItem }) {
  if (!item.subMenu?.length) {
    return (
      <MenuLink url={item.url} className={ENTRY_CLASS}>
        {item.title}
      </MenuLink>
    );
  }

  return (
    <MenubarMenu>
      <MenubarTrigger className={ENTRY_CLASS}>
        {item.title}
        <HugeiconsIcon icon={ArrowDown01Icon} size={14} />
      </MenubarTrigger>
      <MenubarContent align="start">
        {item.subMenu.map((child) => (
          <MenuEntry key={itemKey(child)} item={child} />
        ))}
      </MenubarContent>
    </MenubarMenu>
  );
}

/** A favourite inside an open menu: folders become a submenu, links a plain item. */
function MenuEntry({ item }: { item: MenuItem }) {
  if (!item.subMenu?.length) {
    return (
      <MenubarItem asChild>
        <MenuLink url={item.url} className={MENU_HOVER_CLASS}>
          {item.title}
        </MenuLink>
      </MenubarItem>
    );
  }

  return (
    <MenubarSub>
      <MenubarSubTrigger className={MENU_HOVER_CLASS}>
        {item.title}
      </MenubarSubTrigger>
      <MenubarSubContent>
        {item.subMenu.map((child) => (
          <MenuEntry key={itemKey(child)} item={child} />
        ))}
      </MenubarSubContent>
    </MenubarSub>
  );
}

function itemKey(item: MenuItem): string {
  return item.key ?? item.url ?? item.title;
}
