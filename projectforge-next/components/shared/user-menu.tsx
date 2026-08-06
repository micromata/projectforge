"use client";

import { useTranslations } from "next-intl";
import type { MenuItem } from "@/lib/rs/types";
import { Button } from "@/components/ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { MenuLink } from "@/components/shared/menu-link";

/** "My account" menu on the right of the nav bar; logout is handled in-app, not as a link. */
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
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <Button
          variant="ghost"
          size="sm"
          className="max-w-40 shrink-0"
          aria-label={t("myAccount")}
        >
          <span className="truncate">{username}</span>
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end">
        {items.map((item) => {
          if (item.url === "/logout" || item.key === "LOGOUT") {
            return (
              <DropdownMenuItem key="logout" onSelect={onLogout}>
                {item.title}
              </DropdownMenuItem>
            );
          }
          return (
            <DropdownMenuItem key={item.key ?? item.url ?? item.title} asChild>
              <MenuLink url={item.url}>{item.title}</MenuLink>
            </DropdownMenuItem>
          );
        })}
      </DropdownMenuContent>
    </DropdownMenu>
  );
}
