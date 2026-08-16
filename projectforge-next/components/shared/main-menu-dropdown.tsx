"use client";

import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { Menu01Icon } from "@hugeicons/core-free-icons";
import { balanceMenuColumns } from "@/lib/menu-columns";
import { cn } from "@/lib/utils";
import { useMediaQuery } from "@/hooks/use-media-query";
import type { MenuItem } from "@/lib/rs/types";
import { buttonVariants } from "@/components/ui/button";
import {
  MenubarContent,
  MenubarItem,
  MenubarMenu,
  MenubarTrigger,
} from "@/components/ui/menubar";
import { MENU_HOVER_CLASS, MenuLink } from "@/components/shared/menu-link";
import { useReportMenuUsage } from "@/hooks/use-report-menu-usage";

/**
 * Column count and panel width come from the same place: with the two split up, the panel used to
 * render four columns into 600px on any desktop, wrapping every longer entry title.
 */
const LAYOUTS = {
  xl: { columns: 4, widthClass: "w-[1040px]" },
  lg: { columns: 3, widthClass: "w-[800px]" },
  sm: { columns: 2, widthClass: "w-[560px]" },
  base: { columns: 1, widthClass: "w-[calc(100vw-1.5rem)]" },
} as const;

function useMenuLayout() {
  const isXl = useMediaQuery("(min-width: 1280px)");
  const isLg = useMediaQuery("(min-width: 1024px)");
  const isSm = useMediaQuery("(min-width: 640px)");
  if (isXl) return LAYOUTS.xl;
  if (isLg) return LAYOUTS.lg;
  if (isSm) return LAYOUTS.sm;
  return LAYOUTS.base;
}

/** The full menu tree, grouped into balanced columns. Belongs inside the nav's `Menubar`. */
export function MainMenuDropdown({ categories }: { categories: MenuItem[] }) {
  const t = useTranslations("menu");
  const { columns, widthClass } = useMenuLayout();
  const balanced = balanceMenuColumns(categories, columns);

  if (balanced.length === 0) return null;

  return (
    <MenubarMenu>
      <MenubarTrigger
        className={cn(
          buttonVariants({ variant: "ghost", size: "sm" }),
          "shrink-0 cursor-pointer"
        )}
        aria-label={t("main.title")}
      >
        <HugeiconsIcon icon={Menu01Icon} size={16} />
        <span className="hidden sm:inline">{t("main.title")}</span>
      </MenubarTrigger>
      <MenubarContent
        align="start"
        collisionPadding={12}
        className={cn(
          widthClass,
          // overflow-y-auto overrides the primitive's overflow-hidden so tall menus can scroll.
          "max-h-[min(80vh,var(--radix-menubar-content-available-height))] max-w-[calc(100vw-1.5rem)] overflow-y-auto p-3"
        )}
      >
        <div className="flex items-start gap-6">
          {balanced.map((column, index) => (
            <div
              key={index}
              // basis-0: the balanced columns share the width evenly. Sized by content they would
              // let the column with the longest titles squeeze all others.
              className="flex min-w-0 flex-1 basis-0 flex-col gap-4"
            >
              {column.map((category) => (
                <CategoryColumn
                  key={category.id ?? category.title}
                  category={category}
                />
              ))}
            </div>
          ))}
        </div>
      </MenubarContent>
    </MenubarMenu>
  );
}

function CategoryColumn({ category }: { category: MenuItem }) {
  const report = useReportMenuUsage();
  return (
    <div className="flex flex-col gap-0.5">
      {/* The brand token, not text-primary: --primary turns near-white in dark mode. */}
      <span className="truncate px-2 py-1 text-xs font-semibold tracking-wide text-brand-teal uppercase">
        {category.title}
      </span>
      {category.subMenu?.map((item) => (
        // MenubarItem, not a bare link: it is what closes the panel on click and wires up
        // keyboard navigation.
        <MenubarItem key={item.key ?? item.url ?? item.title} asChild>
          <MenuLink
            url={item.url}
            onClick={() => report(item.key)}
            className={cn("text-sm", MENU_HOVER_CLASS)}
          >
            <span className="truncate">{item.title}</span>
            {item.badge?.counter ? (
              <span className="ml-auto inline-flex h-5 min-w-5 shrink-0 items-center justify-center rounded-full bg-primary px-1 text-xs text-primary-foreground">
                {item.badge.counter}
              </span>
            ) : null}
          </MenuLink>
        </MenubarItem>
      ))}
    </div>
  );
}
