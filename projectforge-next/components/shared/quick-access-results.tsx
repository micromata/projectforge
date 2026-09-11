"use client";

import { useMemo } from "react";
import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { ArrowRight01Icon } from "@hugeicons/core-free-icons";
import { useMenu } from "@/hooks/use-menu";
import { useRecentMenuEntries } from "@/hooks/use-recent-menu-entries";
import {
  flattenMenuEntries,
  groupMenuEntries,
  searchMenuEntries,
  type MenuEntry,
} from "@/lib/menu-search";
import { useNavigateMenuUrl } from "@/hooks/use-navigate-menu-url";
import {
  CommandEmpty,
  CommandGroup,
  CommandItem,
  CommandList,
} from "@/components/ui/command";
import { HighlightedText } from "@/components/shared/highlighted-text";
import { QuickDataHits } from "@/components/shared/quick-data-hits";

/** The full-text search over the business data, i.e. the entry MenuItemDefId.SEARCH points at (now migrated to next). */
const DATA_SEARCH_URL = "next/search";

/** Only ever a cmdk item value, never shown: the recents render under a translated heading. */
const RECENT_GROUP = "recent";

/**
 * The hits of the quick access search: every menu entry, narrowed by the term, one Enter away.
 *
 * Searches the menu the client already has — `useMenu` holds the tree access-filtered and translated
 * by the server (MenuRest), so no request is made for a keystroke and nothing is offered that the
 * user may not open.
 *
 * The ranking is [searchMenuEntries]'s. cmdk's own scoring is switched off by the `shouldFilter`
 * of the `Command` in QuickAccessSearch, which is also the context this list belongs to.
 */
export function QuickAccessResults({
  term,
  onNavigate,
}: {
  term: string;
  /** Called once a destination is chosen, so the search slot can collapse again. */
  onNavigate: () => void;
}) {
  const t = useTranslations("menu");
  // "Favoriten" is no menu text: the bundle has it as a term of its own (`favorites`), used
  // wherever a list offers its saved filters.
  const tRoot = useTranslations();
  const navigate = useNavigateMenuUrl();
  const { data: menu } = useMenu();
  const { recentKeys, remember } = useRecentMenuEntries();

  const entries = useMemo(
    () =>
      flattenMenuEntries(menu, {
        mainMenu: t("main.title"),
        // `._`: the key has children (favorites.saveModification), so it is nested under `_`.
        favorites: tRoot("favorites._"),
        myAccount: t("myAccount"),
      }),
    [menu, t, tRoot]
  );
  const found = useMemo(
    () => searchMenuEntries(entries, term),
    [entries, term]
  );
  const groups = useMemo(() => groupMenuEntries(found), [found]);
  // Only without a search term: once the user types, the ranking is the answer to the question and
  // the history would push a worse match above a better one.
  const recent = term.trim()
    ? []
    : recentKeys
        .map((key) => entries.find((entry) => entry.key === key))
        .filter((entry): entry is MenuEntry => entry !== undefined);

  function go(url: string, menuKey?: string) {
    navigate(url, () => {
      remember(menuKey);
      onNavigate();
    });
  }

  return (
    <>
      {/* Scrolls on its own (`flex-1 min-h-0`); the "search all data" row below is pinned outside it,
          so it stays visible however many hits fill the list — see the footer after this. */}
      <CommandList className="max-h-none min-h-0 flex-1">
        {/* Reached while the menu is still being fetched; a term without a hit is answered by the
            data search row below instead. */}
        {found.length === 0 && !term.trim() && <CommandEmpty />}
        {recent.length > 0 && (
          <CommandGroup heading={t("quickAccess.recent")}>
            {recent.map((entry) => (
              <QuickAccessItem
                key={entry.key}
                group={RECENT_GROUP}
                entry={entry}
                term={term}
                onSelect={go}
              />
            ))}
          </CommandGroup>
        )}
        {groups.map((group) => (
          <CommandGroup key={group.category} heading={group.category}>
            {group.entries.map((entry) => (
              <QuickAccessItem
                key={entry.key}
                group={group.category}
                entry={entry}
                term={term}
                onSelect={go}
              />
            ))}
          </CommandGroup>
        ))}
        {/* The live data hits sit below the menu: the menu is the primary answer, the data the next
          best, and the full search (pinned below) the fallback. */}
        {term.trim() && <QuickDataHits term={term} onNavigate={onNavigate} />}
      </CommandList>
      {/* Pinned below the scroll area, not the last row inside it: the way out of a dead end must stay
          on screen even when the hits above fill the list and scroll it. What the user typed may well
          be a customer's name rather than a menu entry. Outside the CommandList it is no cmdk item, so
          arrow keys skip it — a click or Tab reaches it, as a footer action should. */}
      {term.trim() && (
        <div className="shrink-0 border-t p-1">
          <button
            type="button"
            onClick={() =>
              go(`${DATA_SEARCH_URL}?q=${encodeURIComponent(term.trim())}`)
            }
            className="flex w-full cursor-pointer items-center gap-2 rounded-md px-2 py-1.5 text-left text-sm hover:bg-accent"
          >
            <HugeiconsIcon icon={ArrowRight01Icon} />
            <span className="truncate">
              {t("quickAccess.searchAllData", { arg0: term.trim() })}
            </span>
          </button>
        </div>
      )}
    </>
  );
}

function QuickAccessItem({
  group,
  entry,
  term,
  onSelect,
}: {
  /** Which group renders it — see the `value` below. */
  group: string;
  entry: MenuEntry;
  /** The typed term, highlighted in the title (empty for the term-less recents). */
  term: string;
  onSelect: (url: string, menuKey?: string) => void;
}) {
  return (
    <CommandItem
      // cmdk identifies an item by its value, and without a search term a recent entry is on
      // screen twice: once under "recently used" and once in its category. With one value for both
      // rows, arrow keys would select them together and skip one of them.
      value={`${group}:${entry.key}`}
      onSelect={() => onSelect(entry.url, entry.menuKey)}
    >
      <span className="truncate">
        <HighlightedText text={entry.title} query={term} />
      </span>
      {entry.badgeCounter ? (
        <span className="ml-auto inline-flex h-5 min-w-5 shrink-0 items-center justify-center rounded-full bg-primary px-1 text-xs text-primary-foreground">
          {entry.badgeCounter}
        </span>
      ) : null}
    </CommandItem>
  );
}
