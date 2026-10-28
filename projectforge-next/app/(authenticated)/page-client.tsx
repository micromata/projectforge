"use client";

import { useRouter } from "next/navigation";
import { useEffect } from "react";
import { PageShell } from "@/components/shared/page-shell";
import { useFeatureEnabled } from "@/hooks/use-feature-enabled";
import { useMenu } from "@/hooks/use-menu";
import { resolveMenuUrl } from "@/lib/menu-url";
import type { MenuItem } from "@/lib/rs/types";

/** The feature of the calendar (`MenuItemDefId.CALENDAR`). */
const CALENDAR_FEATURE = "CALENDAR";

/** The first entry of the menu leading to a page of this app. */
function firstPage(items: MenuItem[] | undefined): string | undefined {
  for (const item of items ?? []) {
    if (item.url) {
      const target = resolveMenuUrl(item.url);
      if (target.kind === "internal" && target.href !== "#") return target.href;
    }
    const href = firstPage(item.subMenu);
    if (href) return href;
  }
  return undefined;
}

/**
 * The start page (/next) has no content of its own; it forwards to the calendar, or, with the calendar switched
 * off (projectforge.features.*), to the first page of the user's favourites or main menu.
 *
 * A client-side replace is used on purpose: prod ships as a static export with no
 * server, so a runtime `redirect()` is not available. `replace` (not `push`) keeps
 * the empty start page out of the history, and the basePath (/next) is prepended
 * automatically, so the target is the bare "/calendar".
 */
export function HomePageClient() {
  const router = useRouter();
  const calendar = useFeatureEnabled(CALENDAR_FEATURE);
  const { data: menu } = useMenu();

  useEffect(() => {
    if (calendar) {
      router.replace("/calendar");
      return;
    }
    if (!menu) return;
    const href =
      firstPage(menu.favoritesMenu?.menuItems) ??
      firstPage(menu.mainMenu?.menuItems);
    if (href) router.replace(href);
  }, [calendar, menu, router]);

  return <PageShell>{null}</PageShell>;
}
