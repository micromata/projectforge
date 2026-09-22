"use client";

import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { ArrowDown01Icon, ArrowRight01Icon } from "@hugeicons/core-free-icons";
import { Card } from "@/components/ui/card";
import {
  Collapsible,
  CollapsibleContent,
  CollapsibleTrigger,
} from "@/components/ui/collapsible";
import { Skeleton } from "@/components/ui/skeleton";
import { HighlightedText } from "@/components/shared/highlighted-text";
import { useNavigateMenuUrl } from "@/hooks/use-navigate-menu-url";
import {
  areaMoreUrl,
  type SearchArea,
  type SearchAreaResult,
} from "@/lib/rs/search";
import { cn } from "@/lib/utils";

/**
 * One search area as a tile on the search page. Its collapse state replaces the former scope checkbox:
 * open = the area is in the query and its hits show here; collapsed = the area is dropped from the
 * query (not searched, hence no hit count). Expanding re-runs the search including this area.
 *
 * Every open tile ends in a "show all in …" row into the area's native list, pre-filtered with the term
 * (via [areaMoreUrl]) — present whether or not the area has hits here, so the full list is always one
 * click away. The hit rows open a record via [useNavigateMenuUrl] (next internal, react/wa full load).
 */
export function SearchAreaTile({
  area,
  result,
  term,
  open,
  onOpenChange,
  isFetching,
}: {
  area: SearchArea;
  /** The area's hits, or undefined when the settled query returned none for it. */
  result: SearchAreaResult | undefined;
  term: string;
  /** Whether the area is in scope (tile expanded). */
  open: boolean;
  onOpenChange: (open: boolean) => void;
  /** The search query is in flight — show a body skeleton while no result is in yet. */
  isFetching: boolean;
}) {
  const t = useTranslations("search");
  const navigate = useNavigateMenuUrl();

  const enabled = term.trim().length >= 2;
  const count = result
    ? `${result.hits.length}${result.hasMore ? "+" : ""}`
    : null;

  return (
    <Card
      size="sm"
      className="gap-0 overflow-hidden border py-0 leading-normal shadow-sm"
    >
      <Collapsible open={open} onOpenChange={onOpenChange}>
        <CollapsibleTrigger asChild>
          <button
            type="button"
            className={cn(
              "flex w-full items-center gap-2 bg-muted/40 px-3 py-2.5 text-left transition-colors hover:bg-muted/70",
              open && "border-b"
            )}
          >
            <span className="truncate font-heading text-sm font-medium text-primary">
              {area.title}
            </span>
            {open && count ? (
              <span className="ml-auto shrink-0 rounded-full bg-muted px-1.5 py-0.5 text-[11px] font-medium text-muted-foreground">
                {count}
              </span>
            ) : null}
            <HugeiconsIcon
              icon={ArrowDown01Icon}
              size={16}
              className={cn(
                "shrink-0 text-muted-foreground transition-transform",
                open && count ? "ml-1" : "ml-auto",
                !open && "-rotate-90"
              )}
            />
          </button>
        </CollapsibleTrigger>
        <CollapsibleContent>
          {isFetching && !result ? (
            <div className="flex flex-col gap-1.5 p-3">
              {Array.from({ length: 3 }).map((_, i) => (
                <Skeleton key={i} className="h-5 w-full" />
              ))}
            </div>
          ) : result ? (
            <ul className="flex flex-col py-1">
              {result.hits.map((hit) => (
                <li key={hit.id}>
                  <button
                    type="button"
                    className="flex w-full items-center gap-2 px-3 py-1 text-left text-sm hover:bg-accent"
                    onClick={() => navigate(hit.viewUrl)}
                  >
                    <span className="truncate">
                      <HighlightedText text={hit.displayName} query={term} />
                    </span>
                    {hit.secondaryInfo ? (
                      <span className="ml-auto truncate text-xs text-muted-foreground">
                        {hit.secondaryInfo}
                      </span>
                    ) : null}
                  </button>
                </li>
              ))}
            </ul>
          ) : enabled ? (
            <p className="px-3 py-3 text-sm text-muted-foreground">
              {t("next.noResults")}
            </p>
          ) : null}
          <button
            type="button"
            className="flex w-full items-center gap-1 border-t px-3 py-2 text-left text-xs text-muted-foreground transition-colors hover:bg-accent hover:text-foreground"
            onClick={() => navigate(areaMoreUrl(area, term))}
          >
            <HugeiconsIcon icon={ArrowRight01Icon} size={12} />
            {t("next.showAll", { arg0: area.title })}
          </button>
        </CollapsibleContent>
      </Collapsible>
    </Card>
  );
}
