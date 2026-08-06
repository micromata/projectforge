"use client";

import { useTranslations } from "next-intl";
import { cn } from "@/lib/utils";
import type { MenuItem } from "@/lib/rs/types";
import { buttonVariants } from "@/components/ui/button";
import {
  MenubarContent,
  MenubarItem,
  MenubarMenu,
  MenubarTrigger,
} from "@/components/ui/menubar";
import { MENU_HOVER_CLASS, MenuLink } from "@/components/shared/menu-link";

/**
 * "My account" menu on the right of the nav bar; logout is handled in-app, not as a link.
 * Belongs inside the nav's `Menubar`.
 */
export function UserMenu({
  items,
  username,
  onLogout,
}: {
  items: MenuItem[];
  username: string;
  onLogout: () => void;
}) {
  const t = useTranslations("menu");

  return (
    <MenubarMenu>
      <MenubarTrigger
        className={cn(
          buttonVariants({ variant: "ghost", size: "sm" }),
          "max-w-40 shrink-0 cursor-pointer"
        )}
        aria-label={t("myAccount")}
      >
        <span className="truncate">{username}</span>
      </MenubarTrigger>
      <MenubarContent align="end">
        {items.map((item) => {
          if (item.url === "/logout" || item.key === "LOGOUT") {
            return (
              <MenubarItem
                key="logout"
                className={MENU_HOVER_CLASS}
                onSelect={onLogout}
              >
                {item.title}
              </MenubarItem>
            );
          }
          return (
            <MenubarItem key={item.key ?? item.url ?? item.title} asChild>
              <MenuLink url={item.url} className={MENU_HOVER_CLASS}>
                {item.title}
              </MenuLink>
            </MenubarItem>
          );
        })}
      </MenubarContent>
    </MenubarMenu>
  );
}
