"use client";

import { useTranslations } from "next-intl";
import { Skeleton } from "@/components/ui/skeleton";
import type { SearchResponse } from "@/lib/rs/search";
import { SearchResultGroup } from "@/components/features/search/search-result-group";

/**
 * The grouped hits of the search page: one [SearchResultGroup] per area with matches, in the backend's
 * priority order (addresses first). While a query runs the previous result is kept (the page's
 * `placeholderData`), so the list doesn't blank out between keystrokes — the skeleton only shows on
 * the very first load, and the empty note only once a settled query came back with nothing.
 */
export function SearchResults({
  data,
  term,
  isLoading,
}: {
  data: SearchResponse | undefined;
  term: string;
  isLoading: boolean;
}) {
  const t = useTranslations("search");
  const areas = data?.areas ?? [];

  if (isLoading && areas.length === 0) {
    return (
      <div className="flex flex-1 flex-col gap-3">
        {Array.from({ length: 4 }).map((_, i) => (
          <Skeleton key={i} className="h-16 w-full" />
        ))}
      </div>
    );
  }

  if (term.trim() && areas.length === 0) {
    return (
      <p className="flex-1 py-8 text-center text-sm text-muted-foreground">
        {t("next.noResults")}
      </p>
    );
  }

  return (
    <div className="flex flex-1 flex-col gap-5">
      {areas.map((area) => (
        <SearchResultGroup key={area.areaId} area={area} term={term} />
      ))}
    </div>
  );
}
