"use client";

import { GuardedLink } from "@/components/shared/guarded-link";
import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { UserGroupIcon } from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import { ProjectWizardLink } from "@/components/shared/tasks/project-wizard-link";
import { useMenu } from "@/hooks/use-menu";
import type { MenuItem } from "@/lib/rs/types";

/** The main-menu entry of the customer groups editor (MenuItemDefId.CUSTOMER_GROUPS). */
const CUSTOMER_GROUPS_MENU_ID = "CUSTOMER_GROUPS";

/**
 * The actions of the customer list: the customer wizard, which sets up a customer's project with its
 * structure element, groups and access rights (see ProjectWizardLink, hidden for users who may not run
 * it), and the way to the editor of the customer groups and business units, which is where customers
 * are grouped.
 *
 * The editor is shown only if the main menu offers it: the menu is the server's answer to who may open
 * it (finance and controlling, see CustomerGroupPageRest), so a link that could only answer 403 is
 * not offered.
 */
export function CustomerListActions() {
  const t = useTranslations();
  const { data: menu } = useMenu();

  return (
    <>
      <ProjectWizardLink />
      {hasItem(menu?.mainMenu.menuItems, CUSTOMER_GROUPS_MENU_ID) && (
        <Button asChild variant="outline" size="sm" className="gap-1.5">
          <GuardedLink href="/customerGroups?returnTo=/customer">
            <HugeiconsIcon icon={UserGroupIcon} size={14} aria-hidden />
            {t("fibu.customerGroups.title")}
          </GuardedLink>
        </Button>
      )}
    </>
  );
}

function hasItem(items: MenuItem[] | undefined, id: string): boolean {
  return !!items?.some((item) => item.id === id || hasItem(item.subMenu, id));
}
