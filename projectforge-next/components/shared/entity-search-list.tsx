"use client";

import { useRef, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import {
  Command,
  CommandEmpty,
  CommandInput,
  CommandItem,
  CommandList,
} from "@/components/ui/command";
import { fetchAutoCompletion } from "@/lib/rs/dynamic";
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
  /** Characters before the lookup fires; the backend defaults it to 2. */
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
 * no interest once the entry is found.
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

  const { data: found, isFetching } = useQuery({
    queryKey: ["entity-autocomplete", url, search, params ?? null],
    queryFn: ({ signal }) =>
      fetchAutoCompletion<T>(url, search, params, signal),
    // Below minChars the backend would answer with everything it has, which is neither useful nor
    // cheap — `user/autosearch` without a term lists every active user.
    enabled: active && search.trim().length >= minChars,
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
      <CommandList>
        <CommandEmpty>
          {search.trim().length < minChars
            ? t("filter.search")
            : isFetching
              ? t("loading")
              : t("nothingFound")}
        </CommandEmpty>
        {(found ?? []).map((entry) => (
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
      </CommandList>
    </Command>
  );
}
