"use client";

import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { Search01Icon } from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
import { Skeleton } from "@/components/ui/skeleton";
import type { SearchArea } from "@/lib/rs/search";

/**
 * The scope of the search: one checkbox per area the user may search, plus the "search all areas"
 * (Weitersuchen) shortcut that ticks every box at once.
 *
 * Presentational — the selection lives in the search page, which turns it into the query's `areas`.
 * The areas come already access-filtered and in priority order from the backend (addresses first),
 * so this only renders them. Persisting the selection as the user's default is Phase 2; for now it
 * resets to all areas on every visit.
 */
export function SearchScopePanel({
  areas,
  isLoading,
  selected,
  onToggle,
  onSelectAll,
}: {
  areas: SearchArea[];
  isLoading: boolean;
  /** The currently ticked area ids. */
  selected: Set<string>;
  onToggle: (areaId: string, checked: boolean) => void;
  onSelectAll: () => void;
}) {
  const t = useTranslations("search");
  const allSelected =
    areas.length > 0 && areas.every((a) => selected.has(a.areaId));

  return (
    <aside className="flex w-56 shrink-0 flex-col gap-3">
      <div className="flex items-center justify-between">
        <h2 className="text-sm font-semibold">{t("next.scope")}</h2>
        <Button
          variant="ghost"
          size="sm"
          className="h-7 px-2 text-xs"
          disabled={isLoading || allSelected}
          onClick={onSelectAll}
        >
          <HugeiconsIcon icon={Search01Icon} size={14} />
          {t("next.searchMore")}
        </Button>
      </div>
      {isLoading ? (
        <div className="flex flex-col gap-2">
          {Array.from({ length: 6 }).map((_, i) => (
            <Skeleton key={i} className="h-6 w-full" />
          ))}
        </div>
      ) : (
        <ul className="flex flex-col gap-1.5">
          {areas.map((area) => (
            <li key={area.areaId}>
              <label className="flex cursor-pointer items-center gap-2 text-sm">
                <Checkbox
                  checked={selected.has(area.areaId)}
                  onCheckedChange={(checked) =>
                    onToggle(area.areaId, checked === true)
                  }
                />
                <span className="truncate">{area.title}</span>
              </label>
            </li>
          ))}
        </ul>
      )}
    </aside>
  );
}
