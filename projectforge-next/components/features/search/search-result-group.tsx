"use client";

import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { ArrowRight01Icon } from "@hugeicons/core-free-icons";
import { HighlightedText } from "@/components/shared/highlighted-text";
import { useNavigateMenuUrl } from "@/hooks/use-navigate-menu-url";
import { areaMoreUrl, type SearchAreaResult } from "@/lib/rs/search";

/**
 * The hits of one area on the search page: a heading with the area title and the matching records
 * below it, each a row that opens its record via [useNavigateMenuUrl] (next internal, react/wa full
 * load, unsaved-changes guard included). When the area holds more than the fetched limit, a trailing
 * row leads to the area's native list pre-filtered with the term ([areaMoreUrl]), where the full
 * result set is browsable.
 */
export function SearchResultGroup({
  area,
  term,
}: {
  area: SearchAreaResult;
  term: string;
}) {
  const t = useTranslations("search");
  const navigate = useNavigateMenuUrl();

  return (
    <section className="flex flex-col gap-1">
      <h3 className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">
        {area.title}
      </h3>
      <ul className="flex flex-col">
        {area.hits.map((hit) => (
          <li key={hit.id}>
            <button
              type="button"
              className="flex w-full items-center gap-2 rounded-md px-2 py-1.5 text-left text-sm hover:bg-accent"
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
      {area.hasMore && (
        <button
          type="button"
          className="flex items-center gap-1 rounded-md px-2 py-1.5 text-left text-xs text-muted-foreground hover:bg-accent hover:text-foreground"
          onClick={() => navigate(areaMoreUrl(area, term))}
        >
          <HugeiconsIcon icon={ArrowRight01Icon} size={12} />
          {t("next.moreInArea", { arg0: area.title })}
        </button>
      )}
    </section>
  );
}
