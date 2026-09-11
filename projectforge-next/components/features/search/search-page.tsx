"use client";

import { useEffect, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { useTranslations } from "next-intl";
import { keepPreviousData, useQuery } from "@tanstack/react-query";
import { PageShell } from "@/components/shared/page-shell";
import { PageTitleRow } from "@/components/shared/page-title-row";
import { SearchInput } from "@/components/shared/list/search-input";
import {
  fetchSearchAreas,
  searchData,
  type SearchArea,
  type SearchResponse,
} from "@/lib/rs/search";
import { SearchScopePanel } from "@/components/features/search/search-scope-panel";
import { SearchResults } from "@/components/features/search/search-results";

/** Larger than the magnifier's few hits: the page has room to list a page's worth per area. */
const MAX_PER_AREA = 10;
const MIN_TERM_LENGTH = 2;

/**
 * The dedicated global search page (`/next/search`), the successor of Wicket's `wa/search`.
 *
 * Deep-linkable: `?q=` seeds the search field and `?areas=` (comma-separated) the scope, so the
 * magnifier's "more in …" rows and "search all data" land here pre-scoped. The scope defaults to every
 * accessible area (persisting a user default is Phase 2). The term is mirrored back into the url so a
 * bookmark and the back button keep working; typing is already debounced by [SearchInput].
 */
export function SearchPage() {
  const t = useTranslations("search");
  const router = useRouter();
  const params = useSearchParams();

  const [term, setTerm] = useState(() => params.get("q") ?? "");
  // `null` is the default scope — every accessible area. A url `?areas=` pins an explicit set instead,
  // and once the user (un)ticks a box the set becomes theirs. Kept as-is rather than expanded to all
  // areas on load, so no effect has to write state back and cascade a render.
  const [selected, setSelected] = useState<Set<string> | null>(() => {
    const fromUrl = params.get("areas")?.split(",").filter(Boolean) ?? [];
    return fromUrl.length > 0 ? new Set(fromUrl) : null;
  });

  const areasQuery = useQuery<SearchArea[]>({
    queryKey: ["searchAreas"],
    queryFn: ({ signal }) => fetchSearchAreas(signal),
  });

  // Mirror the term into the url without stacking history entries. The path is app-relative: the
  // router prepends the base path (`/next`) itself, so `/next/search` here would double it.
  useEffect(() => {
    const query = term.trim() ? `?q=${encodeURIComponent(term.trim())}` : "";
    router.replace(`/search${query}`, { scroll: false });
  }, [term, router]);

  const areas = areasQuery.data ?? [];
  // What the checkboxes show: the explicit set, or — in the default — every area ticked.
  const effectiveSelected = selected ?? new Set(areas.map((a) => a.areaId));
  // The query's scope: `undefined` (all) in the default, else the explicit ids.
  const scope = selected ? [...selected].sort() : undefined;
  const enabled = term.trim().length >= MIN_TERM_LENGTH;
  const searchQuery = useQuery<SearchResponse>({
    queryKey: ["search", term.trim(), scope],
    queryFn: ({ signal }) =>
      searchData(term.trim(), scope, MAX_PER_AREA, signal),
    enabled,
    placeholderData: keepPreviousData,
  });

  return (
    <PageShell>
      <div className="bg-background pb-2.5">
        <PageTitleRow
          category={t("title")}
          title={t("search")}
          legacyUrl="wa/search"
          center={
            <div className="relative max-w-md">
              <SearchInput value={term} onChange={setTerm} />
            </div>
          }
        />
      </div>
      <div className="flex min-h-0 flex-1 gap-6 px-4 pb-6">
        <SearchScopePanel
          areas={areas}
          isLoading={areasQuery.isLoading}
          selected={effectiveSelected}
          onToggle={(areaId, checked) =>
            setSelected((prev) => {
              const next = new Set(prev ?? areas.map((a) => a.areaId));
              if (checked) next.add(areaId);
              else next.delete(areaId);
              return next;
            })
          }
          onSelectAll={() => setSelected(null)}
        />
        <SearchResults
          data={enabled ? searchQuery.data : undefined}
          term={term}
          isLoading={searchQuery.isFetching && enabled}
        />
      </div>
    </PageShell>
  );
}
