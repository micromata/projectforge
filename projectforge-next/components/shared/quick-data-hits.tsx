"use client";

import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { ArrowRight01Icon } from "@hugeicons/core-free-icons";
import { CommandGroup, CommandItem } from "@/components/ui/command";
import { HighlightedText } from "@/components/shared/highlighted-text";
import { useDataSearch } from "@/hooks/use-data-search";
import { useNavigateMenuUrl } from "@/hooks/use-navigate-menu-url";
import { areaMoreUrl } from "@/lib/rs/search";

/**
 * The live data hits under the magnifier: a few matches per area, addresses first, below the menu hits.
 *
 * Renders as `CommandGroup`s so it joins the same cmdk list — with `shouldFilter={false}` on the root the
 * arrow keys walk the items in DOM order, so the menu leads and the data follows. Each hit navigates to its
 * record via [useNavigateMenuUrl], collapsing the search slot on the way; an area with more matches ends in a
 * row into the dedicated search page, scoped to that area.
 */
export function QuickDataHits({
  term,
  onNavigate,
}: {
  term: string;
  /** Collapses the search slot once a destination is chosen. */
  onNavigate: () => void;
}) {
  const t = useTranslations("search");
  const navigate = useNavigateMenuUrl();
  const { data } = useDataSearch(term);

  const areas = data?.areas ?? [];
  if (areas.length === 0) return null;

  return (
    <>
      {areas.map((area) => (
        <CommandGroup key={area.areaId} heading={area.title}>
          {area.hits.map((hit) => (
            <CommandItem
              // Globally unique: a data hit and a menu entry could otherwise share a value and the arrow keys
              // would select them together (see the menu items' `group:key` for the same reason).
              key={`${area.areaId}:${hit.id}`}
              value={`data:${area.areaId}:${hit.id}`}
              onSelect={() => navigate(hit.viewUrl, onNavigate)}
            >
              <span className="truncate">
                <HighlightedText text={hit.displayName} query={term} />
              </span>
              {hit.secondaryInfo ? (
                <span className="ml-auto truncate text-xs text-muted-foreground">
                  {hit.secondaryInfo}
                </span>
              ) : null}
            </CommandItem>
          ))}
          {area.hasMore && (
            <CommandItem
              value={`data-more:${area.areaId}`}
              onSelect={() => navigate(areaMoreUrl(area, term), onNavigate)}
            >
              <HugeiconsIcon icon={ArrowRight01Icon} />
              <span className="truncate">
                {t("next.moreInArea", { arg0: area.title })}
              </span>
            </CommandItem>
          )}
        </CommandGroup>
      ))}
    </>
  );
}
