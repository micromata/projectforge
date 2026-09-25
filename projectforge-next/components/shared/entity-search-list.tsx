"use client";

import { useRef, useState } from "react";
import { useTranslations } from "next-intl";
import {
  Command,
  CommandEmpty,
  CommandGroup,
  CommandInput,
  CommandItem,
  CommandList,
} from "@/components/ui/command";
import { LookupLoadingRow } from "@/components/shared/lookup-loading-row";
import { isNearBottom, useEntityLookup } from "@/hooks/use-entity-lookup";
import type { EntityRef } from "./entity-autocomplete";

export interface EntitySearchListProps<T extends EntityRef = EntityRef> {
  /** The lookup url from the layout, with its literal `:search` placeholder. */
  url: string;
  /**
   * Further request parameters the endpoint reads besides the search term — `{projektId}` narrows
   * `cost2/autosearch` to the cost units of one project (`Kost2PagesRest.queryAutocompleteObjects`).
   *
   * Part of the query key, so a changed value asks again instead of serving the previous answer.
   */
  params?: Record<string, unknown>;
  /** Characters before a *typed* term is looked up; the backend defaults it to 2. */
  minChars?: number;
  /**
   * Whether the popover holding this list is open. While it is not, the lookup must not run: the term
   * of the last visit is still here, and its answer would be fetched for nothing.
   */
  active: boolean;
  /** Called with the picked entry **as the backend sent it** — see [EntityAutocompleteProps.onChange]. */
  onPick: (entry: T) => void;
  /**
   * Whether the cursor stays in the search term after a pick, for a caller that collects several
   * entries (see [EntityMultiAutocomplete]) rather than closing on the first one.
   */
  keepFocus?: boolean;
  /**
   * Quick-picks to offer *before* the user types — the recently used entries (see TaskSearchPopover).
   * Shown as a labelled group above the backend results while the search term is empty, and hidden the
   * moment a term is typed, where the backend's matches are the answer. A pick behaves like any other.
   * Left empty by callers that have none (e.g. EntityAutocomplete), which then renders unchanged.
   */
  recentEntries?: T[];
  /** Heading of the [recentEntries] group; required only when there are recent entries to show. */
  recentLabel?: string;
  /**
   * Whether opening with an empty term asks the backend for a first slice of entries. Default on; the
   * task search passes `false`, where the empty-term answer is a random, mostly useless part of the
   * whole tree and the recents are the "before you type" content instead (see [useEntityLookup]).
   */
  emptyTermSearches?: boolean;
  /**
   * A term to start with, so a picker opened by typing (rather than clicking) keeps that first
   * character instead of dropping it — see [EntityAutocomplete]. The list mounts fresh on each open, so
   * this seeds the term once; the user types on from there.
   */
  initialSearch?: string;
}

/**
 * Searches one entity's `autosearch` and offers what it answers — the body of the picker popovers, shared
 * by the single-value [EntityAutocomplete] and the collecting [EntityMultiAutocomplete].
 *
 * It owns the term and the query alone: what a pick *means* is the caller's business, and the term is of
 * no interest once the entry is found. Which entries there are to offer — the first page as soon as the
 * popover opens, a longer one while the user scrolls — is [useEntityLookup]'s, shared in turn with the
 * picker of a server-laid-out form (DynamicSelect), so both behave the same.
 */
export function EntitySearchList<T extends EntityRef = EntityRef>({
  url,
  params,
  minChars = 2,
  active,
  onPick,
  keepFocus,
  recentEntries,
  recentLabel,
  emptyTermSearches = true,
  initialSearch = "",
}: EntitySearchListProps<T>) {
  const t = useTranslations();
  const [search, setSearch] = useState(initialSearch);
  const searchRef = useRef<HTMLInputElement>(null);

  const { entries, isFetching, isLoadingMore, loadMore } = useEntityLookup<T>({
    url,
    search,
    params,
    open: active,
    minChars,
    emptyTermSearches,
  });

  /** The recent quick-picks only make sense before a term narrows the list; typing takes over. */
  const showRecent =
    search.trim().length === 0 && (recentEntries?.length ?? 0) > 0;

  const pick = (entry: T) => {
    onPick(entry);
    // The term goes either way: what was searched for has been found.
    setSearch("");
    // Explicitly, and not by leaving focus alone: a pick by mouse leaves it on the item that was
    // clicked, so the next term would go nowhere.
    if (keepFocus) searchRef.current?.focus();
  };

  return (
    // The backend does the filtering; cmdk must not filter the results again.
    <Command shouldFilter={false}>
      <CommandInput
        ref={searchRef}
        value={search}
        onValueChange={setSearch}
        placeholder={t("filter.search")}
      />
      {/* cmdk's list is its own scroll container, so the next page is asked for from here. The
          arrow keys scroll it too, which pages for the keyboard as well. */}
      <CommandList
        onScroll={(event) => {
          if (isNearBottom(event.currentTarget)) loadMore();
        }}
      >
        <CommandEmpty>
          {/* Nothing typed yet and still empty means the lookup is either running or has nothing to
              offer — the hint to type only fits a term that is too short to be looked up. */}
          {search.trim().length > 0 && search.trim().length < minChars
            ? t("filter.search")
            : isFetching
              ? t("loading")
              : t("nothingFound")}
        </CommandEmpty>
        {showRecent && (
          // Own value prefix so a recent entry never collides with the same id in the backend results
          // that also load for the empty term (cmdk keys items by their value). No own scroll container:
          // cmdk gives `CommandList` the single scroller, and a nested `overflow-y-auto` here would
          // swallow the wheel without scrolling reliably (its base class is `overflow-hidden`). The
          // whole list scrolls as one; the heading only stays pinned to the list's top while its items
          // pass under it.
          <CommandGroup
            heading={recentLabel}
            className="**:[[cmdk-group-heading]]:sticky **:[[cmdk-group-heading]]:top-0 **:[[cmdk-group-heading]]:bg-popover"
          >
            {recentEntries!.map((entry) => (
              <CommandItem
                key={`recent-${entry.id}`}
                value={`recent-${entry.id}`}
                onSelect={() => pick(entry)}
              >
                {entry.displayName}
              </CommandItem>
            ))}
          </CommandGroup>
        )}
        {/* While the recents are shown (empty term with recents to offer) they *are* the answer, so the
            backend's empty-term dump — for the task lookup the whole tree — stays out: mixing the two
            buried the few recents under it and put two differently-indented item styles in one list. A
            typed term flips showRecent off and the matches take over. Callers without recents
            (EntityAutocomplete) keep showing their entries on the empty term unchanged. */}
        {!showRecent &&
          entries.map((entry) => (
            <CommandItem
              key={entry.id}
              value={String(entry.id)}
              onSelect={() => pick(entry)}
            >
              {entry.displayName}
            </CommandItem>
          ))}
        {isLoadingMore && <LookupLoadingRow />}
      </CommandList>
    </Command>
  );
}
