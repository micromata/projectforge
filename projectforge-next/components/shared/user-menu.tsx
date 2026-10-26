"use client";

import { useTranslations } from "next-intl";
import { usePathname } from "next/navigation";
import { HugeiconsIcon } from "@hugeicons/react";
import { ArrowDown01Icon } from "@hugeicons/core-free-icons";
import { cn } from "@/lib/utils";
import type { MenuItem } from "@/lib/rs/types";
import { buttonVariants } from "@/components/ui/button";
import {
  MenubarContent,
  MenubarItem,
  MenubarMenu,
  MenubarSeparator,
  MenubarTrigger,
} from "@/components/ui/menubar";
import { GuardedLink } from "@/components/shared/guarded-link";
import { MENU_HOVER_CLASS, MenuLink } from "@/components/shared/menu-link";
import { MenuCounterBadge } from "@/components/shared/menu-counter-badge";
import { ThemeMenu } from "@/components/shared/theme-menu";
import { UserAvatar } from "@/components/shared/user-avatar";
import { useReportMenuUsage } from "@/hooks/use-report-menu-usage";

const MY_ACCOUNT_PATH = "/myAccount";
/** The section id of the avatar in MyAccountForm. */
const AVATAR_SECTION = "avatar";
const AVATAR_HREF = `${MY_ACCOUNT_PATH}#${AVATAR_SECTION}`;

/**
 * "My account" menu on the right of the nav bar; logout is handled in-app, not as a link.
 * Belongs inside the nav's `Menubar`.
 *
 * MenuRest builds this menu with a single top-level item carrying the user's name and hangs
 * the real entries below it (see MenuRest.getMenu), so the entries are one level down.
 *
 * The avatar in front of the name is no part of the menu but a link to the avatar section of My account;
 * name and chevron open the menu.
 */
export function UserMenu({
  items,
  username,
  userId,
  avatarVersion,
  onLogout,
}: {
  items: MenuItem[];
  username: string;
  /** The own avatar in front of the name (see UserAvatar). */
  userId?: number | null;
  avatarVersion?: number | null;
  onLogout: () => void;
}) {
  const t = useTranslations("menu");
  const tUser = useTranslations("user");
  const report = useReportMenuUsage();
  // Tolerate a flat menu too: the wrapper is MenuRest's doing, not part of the contract.
  const entries = items.flatMap((item) => item.subMenu ?? [item]);
  // Logout is handled in-app, not as a link, and belongs at the bottom below the appearance toggle.
  const isLogout = (item: MenuItem) =>
    item.key === "LOGOUT" || item.url === "logout";
  const linkEntries = entries.filter((item) => !isLogout(item));
  const hasLogout = entries.some(isLogout);
  const pathname = usePathname();

  return (
    <MenubarMenu>
      <GuardedLink
        href={AVATAR_HREF}
        aria-label={`${t("myAccount")}: ${tUser("avatar._")}`}
        className={cn(
          buttonVariants({ variant: "ghost", size: "icon" }),
          "shrink-0 rounded-full"
        )}
        onClick={(event) => {
          // Already on My account, the link would only replace the url: jump by the hash, which the
          // form follows (see EditPageShell). Set to the same hash, it is announced by hand.
          if (pathname !== MY_ACCOUNT_PATH) return;
          event.preventDefault();
          if (window.location.hash === `#${AVATAR_SECTION}`) {
            window.dispatchEvent(new HashChangeEvent("hashchange"));
          } else {
            window.location.hash = AVATAR_SECTION;
          }
        }}
      >
        <UserAvatar
          name={username}
          userId={userId}
          avatarVersion={avatarVersion}
          className="size-6 text-[10px]"
        />
      </GuardedLink>
      <MenubarTrigger
        className={cn(
          buttonVariants({ variant: "ghost", size: "sm" }),
          "max-w-48 shrink-0 cursor-pointer"
        )}
        aria-label={t("myAccount")}
      >
        <span className="truncate">{username}</span>
        {/* Same icon and size as a favourites folder, so both read as openable. */}
        <HugeiconsIcon icon={ArrowDown01Icon} size={14} className="shrink-0" />
      </MenubarTrigger>
      <MenubarContent align="end">
        {linkEntries.map((item) => (
          <MenubarItem key={item.key ?? item.url ?? item.title} asChild>
            <MenuLink
              url={item.url}
              onClick={() => report(item.key)}
              className={MENU_HOVER_CLASS}
            >
              <span className="truncate">{item.title}</span>
              {/* "2FA setup" carries a counter; without this it would be lost here. */}
              <MenuCounterBadge badge={item.badge} />
            </MenuLink>
          </MenubarItem>
        ))}
        <MenubarSeparator />
        <ThemeMenu />
        {hasLogout && (
          <>
            <MenubarSeparator />
            <MenubarItem className={MENU_HOVER_CLASS} onSelect={onLogout}>
              {entries.find(isLogout)?.title}
            </MenubarItem>
          </>
        )}
      </MenubarContent>
    </MenubarMenu>
  );
}
