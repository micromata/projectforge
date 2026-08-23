"use client";

import { useRef, useState } from "react";
import { useTranslations } from "next-intl";
import {
  Command,
  CommandEmpty,
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
}: EntitySearchListProps<T>) {
  const t = useTranslations();
  const [search, setSearch] = useState("");
  const searchRef = useRef<HTMLInputElement>(null);

  const { entries, isFetching, isLoadingMore, loadMore } = useEntityLookup<T>({
    url,
    search,
    params,
    open: active,
    minChars,
  });

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
        {entries.map((entry) => (
          <CommandItem
            key={entry.id}
            value={String(entry.id)}
            onSelect={() => {
              onPick(entry);
              // The term goes either way: what was searched for has been found.
              setSearch("");
              // Explicitly, and not by leaving focus alone: a pick by mouse leaves it on the item
              // that was clicked, so the next term would go nowhere.
              if (keepFocus) searchRef.current?.focus();
            }}
          >
            {entry.displayName}
          </CommandItem>
        ))}
        {isLoadingMore && <LookupLoadingRow />}
      </CommandList>
    </Command>
  );
}
