"use client";

import Link from "next/link";
import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { UserGroupIcon } from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import { useMenu } from "@/hooks/use-menu";
import type { MenuItem } from "@/lib/rs/types";

/** The main-menu entry of the customer groups editor (MenuItemDefId.CUSTOMER_GROUPS). */
const CUSTOMER_GROUPS_MENU_ID = "CUSTOMER_GROUPS";

/**
 * The way from the customers to the editor of the customer groups and business units, which is
 * where customers are grouped.
 *
 * Shown only if the main menu offers the editor: the menu is the server's answer to who may open
 * it (finance and controlling, see CustomerGroupPageRest), so a link that could only answer 403 is
 * not offered.
 */
export function CustomerListActions() {
  const t = useTranslations();
  const { data: menu } = useMenu();
  if (!hasItem(menu?.mainMenu.menuItems, CUSTOMER_GROUPS_MENU_ID)) return null;

  return (
    <Button asChild variant="outline" size="sm" className="gap-1.5">
      <Link href="/customerGroups?returnTo=/customer">
        <HugeiconsIcon icon={UserGroupIcon} size={14} aria-hidden />
        {t("fibu.customerGroups.title")}
      </Link>
    </Button>
  );
}

function hasItem(items: MenuItem[] | undefined, id: string): boolean {
  return !!items?.some((item) => item.id === id || hasItem(item.subMenu, id));
}
