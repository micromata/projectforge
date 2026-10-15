"use client";

import { useTranslations } from "next-intl";
import { balanceMenuColumns } from "@/lib/menu-columns";
import { cn } from "@/lib/utils";
import { useMediaQuery } from "@/hooks/use-media-query";
import { useMenu } from "@/hooks/use-menu";
import type { MenuItem } from "@/lib/rs/types";
import { MENU_HOVER_CLASS, MenuLink } from "@/components/shared/menu-link";
import { MenuCounterBadge } from "@/components/shared/menu-counter-badge";
import { useReportMenuUsage } from "@/hooks/use-report-menu-usage";

/**
 * Column count and panel width come from the same place: with the two split up, the panel used to
 * render four columns into 600px on any desktop, wrapping every longer entry title. Exported so the
 * menu's `Popover` shell (MainMenuDropdown) can size itself to the same layout the columns use.
 */
const LAYOUTS = {
  xl: { columns: 4, widthClass: "w-[1040px]" },
  lg: { columns: 3, widthClass: "w-[800px]" },
  sm: { columns: 2, widthClass: "w-[560px]" },
  base: { columns: 1, widthClass: "w-[calc(100vw-1.5rem)]" },
} as const;

export function useMenuLayout() {
  const isXl = useMediaQuery("(min-width: 1280px)");
  const isLg = useMediaQuery("(min-width: 1024px)");
  const isSm = useMediaQuery("(min-width: 640px)");
  if (isXl) return LAYOUTS.xl;
  if (isLg) return LAYOUTS.lg;
  if (isSm) return LAYOUTS.sm;
  return LAYOUTS.base;
}

/**
 * The browse state of the main menu: the full tree in balanced columns, shown while the search field
 * is empty, with the "recently used" entries on top. Distributing the categories over columns of
 * roughly equal height is [balanceMenuColumns]'s job.
 *
 * Plain links, not cmdk items: browsing a two-dimensional grid of columns by arrow key is worse than
 * reaching for the mouse, and a keyboard user after a specific entry types for it — which switches the
 * panel to the single-column result list (QuickAccessResults). Each click reports the entry as used
 * and closes the panel via `onNavigate`.
 */
export function MainMenuBrowse({
  categories,
  onNavigate,
}: {
  categories: MenuItem[];
  /** Called once an entry is chosen, so the menu's `Popover` can close again. */
  onNavigate: () => void;
}) {
  const t = useTranslations("menu");
  const { columns } = useMenuLayout();
  const { data: menu } = useMenu();
  const balanced = balanceMenuColumns(categories, columns);
  // The history is the backend's, shared by all three frontends and already resolved against this
  // menu (RecentMenuEntriesService), so the entries are access-filtered and titled for free.
  const recent = menu?.recentMenu?.menuItems ?? [];

  if (balanced.length === 0) return null;

  return (
    <div className="flex min-h-0 flex-1 flex-col gap-4 overflow-y-auto p-3">
      {recent.length > 0 && (
        // A labelled group rather than a bare heading, so the "recently used" strip is announced as
        // one region and reachable as such.
        <div
          role="group"
          aria-label={t("quickAccess.recent")}
          className="flex flex-col gap-0.5"
        >
          <MenuColumnHeading title={t("quickAccess.recent")} />
          <div className="flex flex-wrap gap-x-4">
            {recent.map((item) => (
              <MenuEntryLink
                key={item.key ?? item.url ?? item.title}
                item={item}
                onNavigate={onNavigate}
              />
            ))}
          </div>
        </div>
      )}
      <div className="flex items-start gap-6">
        {balanced.map((column, index) => (
          <div
            key={index}
            // basis-0: the balanced columns share the width evenly. Sized by content they would let
            // the column with the longest titles squeeze all others.
            className="flex min-w-0 flex-1 basis-0 flex-col gap-4"
          >
            {column.map((category) => (
              <CategoryColumn
                key={category.id ?? category.title}
                category={category}
                onNavigate={onNavigate}
              />
            ))}
          </div>
        ))}
      </div>
    </div>
  );
}

function CategoryColumn({
  category,
  onNavigate,
}: {
  category: MenuItem;
  onNavigate: () => void;
}) {
  return (
    <div className="flex flex-col gap-0.5">
      <MenuColumnHeading title={category.title} badge={category.badge} />
      {category.subMenu?.map((item) => (
        <MenuEntryLink
          key={item.key ?? item.url ?? item.title}
          item={item}
          onNavigate={onNavigate}
        />
      ))}
    </div>
  );
}

/** The brand-teal category/section label. Not text-primary: --primary turns near-white in dark mode. */
function MenuColumnHeading({
  title,
  badge,
}: {
  title: string;
  badge?: MenuItem["badge"];
}) {
  return (
    <span className="flex items-center gap-2 px-2 py-1 text-xs font-semibold tracking-wide text-brand-teal uppercase">
      <span className="truncate">{title}</span>
      <MenuCounterBadge badge={badge} />
    </span>
  );
}

/** A single navigable menu entry: reports itself as used on click and closes the panel. */
function MenuEntryLink({
  item,
  onNavigate,
}: {
  item: MenuItem;
  onNavigate: () => void;
}) {
  const report = useReportMenuUsage();
  return (
    <MenuLink
      url={item.url}
      onClick={() => {
        report(item.key);
        onNavigate();
      }}
      className={cn(
        "flex items-center rounded-md px-2 py-1 text-sm",
        MENU_HOVER_CLASS
      )}
    >
      <span className="truncate">{item.title}</span>
      <MenuCounterBadge badge={item.badge} />
    </MenuLink>
  );
}
