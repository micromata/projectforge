"use client";

import { useEffect, useState } from "react";
import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { Menu01Icon } from "@hugeicons/core-free-icons";
import { cn } from "@/lib/utils";
import type { MenuBadge, MenuItem } from "@/lib/rs/types";
import { buttonVariants } from "@/components/ui/button";
import { Command, CommandInput } from "@/components/ui/command";
import {
  Popover,
  PopoverAnchor,
  PopoverContent,
} from "@/components/ui/popover";
import { MenuCounterBadge } from "@/components/shared/menu-counter-badge";
import {
  MainMenuBrowse,
  useMenuLayout,
} from "@/components/shared/main-menu-browse";
import { QuickAccessResults } from "@/components/shared/quick-access-results";

/**
 * The main menu behind the "≡ Menü" trigger: browse the whole tree, or type to search across the
 * menu, the live business data and — as a fallback — the full-text search over all data.
 *
 * One surface, two states. With an empty field the balanced columns of the mega-menu are shown for
 * browsing (MainMenuBrowse); the first keystroke collapses them into the ranked, keyboard-navigable
 * result list of [QuickAccessResults] — the same list the search page and the data search feed, so
 * the two never drift apart. The placeholder is therefore "Suchen", not "Menü durchsuchen": the menu
 * is only the first of the three things this field reaches.
 *
 * A `Popover` + cmdk `Command`, deliberately not a `MenubarMenu` (which it used to be): cmdk needs
 * input and list in one context to let the field's arrow keys and Enter drive the hits, and a Radix
 * menu's own roving focus would fight it. `className="contents"` keeps the cmdk root as the shared
 * context without drawing a box of its own around the nav item.
 */
export function MainMenuDropdown({
  categories,
  badge,
}: {
  categories: MenuItem[];
  badge?: MenuBadge;
}) {
  const t = useTranslations("menu");
  const [open, setOpen] = useState(false);
  const [term, setTerm] = useState("");
  // The panel is as wide as the browse columns need, in every state: switching width when the columns
  // give way to the result list would make the panel jump under the pointer.
  const { widthClass } = useMenuLayout();

  useEffect(() => {
    const handleKeyDown = (event: KeyboardEvent) => {
      if (!(event.metaKey || event.ctrlKey)) return;
      // `event.code` names the physical key, so the shortcut survives a keyboard layout that puts
      // something else on `K`.
      if (event.code !== "KeyK") return;
      event.preventDefault();
      setOpen(true);
    };
    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, []);

  function close() {
    setOpen(false);
    // Every visit starts with an empty field, on the browse columns: the previous term answers a
    // question already answered, and it would hide the menu behind its own hits.
    setTerm("");
  }

  if (categories.length === 0) return null;

  return (
    <Command shouldFilter={false} className="contents">
      <Popover
        open={open}
        onOpenChange={(next) => (next ? setOpen(true) : close())}
      >
        <PopoverAnchor asChild>
          <button
            type="button"
            onClick={() => setOpen(true)}
            aria-label={t("main.title")}
            aria-keyshortcuts="Meta+K Control+K"
            className={cn(
              buttonVariants({ variant: "ghost", size: "sm" }),
              // `relative`: anchors the corner counter badge to this button's top-right.
              "relative shrink-0 cursor-pointer px-1.5"
            )}
          >
            <HugeiconsIcon icon={Menu01Icon} size={16} />
            <span className="hidden sm:inline">{t("main.short")}</span>
            <MenuCounterBadge badge={badge} variant="corner" />
          </button>
        </PopoverAnchor>
        <PopoverContent
          align="start"
          collisionPadding={12}
          // The field keeps the focus on open: the user came to type or to browse, and the list is
          // driven from the field.
          onOpenAutoFocus={(event) => event.preventDefault()}
          className={cn(
            widthClass,
            "flex max-h-[min(80vh,var(--radix-popover-content-available-height))] max-w-[calc(100vw-1.5rem)] flex-col overflow-hidden p-0"
          )}
        >
          <CommandInput
            value={term}
            onValueChange={setTerm}
            placeholder={t("quickAccess.placeholder")}
            autoFocus
            aria-label={t("quickAccess._")}
          />
          {term.trim() ? (
            <QuickAccessResults term={term} onNavigate={close} />
          ) : (
            <MainMenuBrowse categories={categories} onNavigate={close} />
          )}
        </PopoverContent>
      </Popover>
    </Command>
  );
}
