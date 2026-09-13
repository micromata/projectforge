"use client";

import { useEffect, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { useTranslations } from "next-intl";
import { keepPreviousData, useQuery } from "@tanstack/react-query";
import { HugeiconsIcon } from "@hugeicons/react";
import { Search01Icon } from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import { PageShell } from "@/components/shared/page-shell";
import { PageTitleRow } from "@/components/shared/page-title-row";
import { SearchInput } from "@/components/shared/list/search-input";
import {
  fetchSearchAreas,
  searchData,
  type SearchArea,
  type SearchResponse,
} from "@/lib/rs/search";
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
  // Which tiles are expanded: the explicit set, or — in the default — every area open.
  const effectiveSelected = selected ?? new Set(areas.map((a) => a.areaId));
  const allOpen =
    selected === null ||
    (areas.length > 0 && areas.every((a) => effectiveSelected.has(a.areaId)));
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
      <div className="flex min-h-0 flex-1 flex-col gap-3 px-4 pb-6">
        <div className="flex shrink-0 items-center justify-end">
          <Button
            variant="ghost"
            size="sm"
            className="h-7 px-2 text-xs"
            disabled={areasQuery.isLoading || allOpen}
            onClick={() => setSelected(null)}
          >
            <HugeiconsIcon icon={Search01Icon} size={14} />
            {t("next.searchMore")}
          </Button>
        </div>
        <SearchResults
          areas={areas}
          results={enabled ? (searchQuery.data?.areas ?? []) : []}
          term={term}
          openIds={effectiveSelected}
          onToggle={(areaId, open) =>
            setSelected((prev) => {
              const next = new Set(prev ?? areas.map((a) => a.areaId));
              if (open) next.add(areaId);
              else next.delete(areaId);
              return next;
            })
          }
          isLoading={areasQuery.isLoading}
          isFetching={searchQuery.isFetching && enabled}
        />
      </div>
    </PageShell>
  );
}
