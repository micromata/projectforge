"use client";

import { Skeleton } from "@/components/ui/skeleton";
import { SearchAreaTile } from "@/components/features/search/search-area-tile";
import type { SearchArea, SearchAreaResult } from "@/lib/rs/search";

/**
 * The responsive tile grid of the search page: one [SearchAreaTile] per accessible area, in the
 * backend's priority order (addresses first). Every area gets a tile — a collapsed one must stay
 * visible so it can be expanded — so the grid is driven by `areas`, not just the areas with hits.
 * A tile's open state is the area's scope: open = searched (result shown), collapsed = out of query.
 * While a query runs, the previous results are kept (the page's `placeholderData`), so tiles don't
 * blank out between keystrokes.
 */
export function SearchResults({
  areas,
  results,
  term,
  openIds,
  onToggle,
  isLoading,
  isFetching,
}: {
  areas: SearchArea[];
  results: SearchAreaResult[];
  term: string;
  /** The area ids currently in scope (expanded tiles). */
  openIds: Set<string>;
  onToggle: (areaId: string, open: boolean) => void;
  /** The areas list is loading (first paint). */
  isLoading: boolean;
  /** A search query is in flight. */
  isFetching: boolean;
}) {
  if (isLoading && areas.length === 0) {
    return (
      <div className="grid grid-cols-1 items-start gap-4 overflow-y-auto pb-6 sm:grid-cols-2 xl:grid-cols-3">
        {Array.from({ length: 6 }).map((_, i) => (
          <Skeleton key={i} className="h-12 w-full" />
        ))}
      </div>
    );
  }

  return (
    <div className="grid grid-cols-1 items-start gap-4 overflow-y-auto pb-6 sm:grid-cols-2 xl:grid-cols-3">
      {areas.map((area) => (
        <SearchAreaTile
          key={area.areaId}
          area={area}
          result={results.find((r) => r.areaId === area.areaId)}
          term={term}
          open={openIds.has(area.areaId)}
          onOpenChange={(open) => onToggle(area.areaId, open)}
          isFetching={isFetching}
        />
      ))}
    </div>
  );
}
