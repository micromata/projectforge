"use client";

import { useMemo } from "react";
import { useTranslations } from "next-intl";
import { FieldHint } from "@/components/shared/form/field-hint";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import { Badge } from "@/components/ui/badge";
import { useFormatContext } from "@/hooks/use-format";
import { useListMeta } from "@/hooks/use-list-meta";
import { filterValuesFromEntries } from "@/components/data-table/filter-value";
import { useResolvedFilterElements } from "@/components/data-table/use-filter-list-values";
import { filterElementsOf } from "@/lib/rs/filter-elements";
import type { MagicFilter } from "@/lib/rs/types";
import { cn } from "@/lib/utils";
import {
  appliedFilterItems,
  type AppliedFilterItem,
  type AppliedFilterUsage,
} from "./applied-filter-items";

/**
 * The list's filter as a "Grafiken" tab took it over, above the charts: so the user can see which
 * criteria the figures rest on, and why they may differ from the list.
 *
 * Each entry reads as its pill in the list does (see [appliedFilterItems]); the labels come from the
 * list's filter fields (`listMeta`, a cache read). An entry the chart left out ([usage]) is struck
 * through in red, as a deleted row is, with the reason as its tooltip; a replaced one in grey, a partly
 * applied one is tagged. One compact row: the notes and the hint that the table's column filters (the header
 * funnels) never reach a chart sit behind info icons, so long sentences don't break the chips apart.
 */
export function AppliedFilterSummary({
  entity,
  filter,
  usage,
  notes,
  ignoredTooltip,
  className,
}: {
  entity: string;
  filter: MagicFilter | undefined;
  usage?: AppliedFilterUsage;
  /**
   * A short remark after an entry, by field id — what replaced it, or how the chart uses it. On an ignored
   * entry it is the tooltip of its tag instead: why this criterion was left out.
   */
  notes?: Readonly<Record<string, string>>;
  /** Why the chart left a criterion out, where the generic "not supported" says too little. */
  ignoredTooltip?: string;
  className?: string;
}) {
  const t = useTranslations();
  const ctx = useFormatContext();
  const meta = useListMeta(entity);
  // Values loaded on demand (the order book's customers) are fetched to name the picks, as the pill does.
  const elements = useResolvedFilterElements(
    filterElementsOf(meta.data),
    filterValuesFromEntries(filter?.entries)
  );

  const items = useMemo(
    () =>
      appliedFilterItems(
        filter,
        elements,
        usage,
        { history: t("filter.history"), search: t("searchString") },
        ctx
      ),
    [filter, elements, usage, t, ctx]
  );

  const title = t("filter.applied._");
  return (
    <section
      className={cn(
        "flex flex-wrap items-center gap-x-2 gap-y-1.5 rounded-md border bg-muted/30 px-3 py-1.5 text-xs",
        className
      )}
      aria-label={title}
    >
      <span className="inline-flex items-center gap-1.5 font-medium text-muted-foreground">
        {title}
        <FieldHint hint={t("filter.applied.columnFiltersHint")} label={title} />
      </span>
      {items.length === 0 ? (
        <span className="text-muted-foreground">
          {t("filter.applied.none")}
        </span>
      ) : (
        items.map((item) => (
          <SummaryChip
            key={item.key}
            item={item}
            note={notes?.[item.key]}
            ignoredTooltip={ignoredTooltip}
          />
        ))
      )}
    </section>
  );
}

function SummaryChip({
  item,
  note,
  ignoredTooltip,
}: {
  item: AppliedFilterItem;
  note?: string;
  ignoredTooltip?: string;
}) {
  const t = useTranslations("filter.applied");
  const ignored = item.status === "ignored";
  const text = (
    <span
      className={cn(
        "truncate",
        // Struck through in red as a deleted row is (`row-deleted`), the reason behind its tooltip.
        ignored && "line-through decoration-destructive decoration-2",
        item.status === "replaced" && "text-muted-foreground line-through"
      )}
    >
      <span className="text-muted-foreground">{item.label}</span>
      {/* A boolean pill has no value text: its label alone says what is meant. */}
      {item.value && <>: {item.value}</>}
      {/* The strike alone says nothing to a screen reader. */}
      {ignored && <span className="sr-only"> ({t("ignored")})</span>}
    </span>
  );
  return (
    <span className="inline-flex h-6 max-w-full items-center gap-1.5 rounded-full border bg-background px-2.5">
      {/* An ignored criterion's own note says why it is ignored, so it replaces the general reason. */}
      {ignored ? (
        <HintTooltip
          text={note ?? ignoredTooltip ?? t("ignoredTooltip")}
          openOnTap
        >
          {text}
        </HintTooltip>
      ) : (
        text
      )}
      {note && !ignored && <FieldHint hint={note} label={item.label} />}
      {item.status === "partial" && (
        <HintTooltip text={t("partialTooltip")} openOnTap>
          <Badge variant="destructive" className="h-4 px-1.5 text-[10px]">
            {t("partial")}
          </Badge>
        </HintTooltip>
      )}
    </span>
  );
}
