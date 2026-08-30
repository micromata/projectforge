"use client";

import { useMemo } from "react";
import { useRouter } from "next/navigation";
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
import { resolveMenuUrl, toAbsoluteUrl } from "@/lib/menu-url";
import { confirmLeaveUnsavedChanges } from "@/hooks/use-unsaved-changes-warning";
import {
  CommandEmpty,
  CommandGroup,
  CommandItem,
  CommandList,
} from "@/components/ui/command";

/** The full-text search over the business data, i.e. the entry MenuItemDefId.SEARCH points at. */
const DATA_SEARCH_URL = "wa/search";

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
  const router = useRouter();
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
    const target = resolveMenuUrl(url);
    // The legacy React app and Wicket are served by Spring, not by this app: a client-side route
    // would land on Next's own 404, and that is where most menu entries still point. A full page load,
    // which `beforeunload` guards by itself — so it is not asked about here.
    if (target.kind === "external") {
      remember(menuKey);
      onNavigate();
      window.location.assign(toAbsoluteUrl(target));
      return;
    }
    // A router.push is not a link, so nothing else would stop it (see useUnsavedChangesWarning): ask
    // with the app's own dialog first and go through only on "leave".
    void confirmLeaveUnsavedChanges().then((leave) => {
      if (!leave) return;
      remember(menuKey);
      onNavigate();
      router.push(target.href);
    });
  }

  return (
    <CommandList>
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
              onSelect={go}
            />
          ))}
        </CommandGroup>
      ))}
      {term.trim() && (
        <CommandGroup>
          {/* The way out of a dead end: what the user typed may well be a customer's name rather
              than a menu entry. */}
          <CommandItem
            value="__data-search__"
            onSelect={() =>
              go(
                `${DATA_SEARCH_URL}?searchString=${encodeURIComponent(term.trim())}`
              )
            }
          >
            <HugeiconsIcon icon={ArrowRight01Icon} />
            <span className="truncate">
              {t("quickAccess.searchAllData", { arg0: term.trim() })}
            </span>
          </CommandItem>
        </CommandGroup>
      )}
    </CommandList>
  );
}

function QuickAccessItem({
  group,
  entry,
  onSelect,
}: {
  /** Which group renders it — see the `value` below. */
  group: string;
  entry: MenuEntry;
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
      <span className="truncate">{entry.title}</span>
      {entry.badgeCounter ? (
        <span className="ml-auto inline-flex h-5 min-w-5 shrink-0 items-center justify-center rounded-full bg-primary px-1 text-xs text-primary-foreground">
          {entry.badgeCounter}
        </span>
      ) : null}
    </CommandItem>
  );
}
