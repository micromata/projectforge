"use client";

import { useMemo } from "react";
import { useTranslations } from "next-intl";
import { useListMeta } from "@/hooks/use-list-meta";
import { filterElementsOf } from "@/lib/rs/filter-elements";
import { filterFieldLabel } from "./applied-filter-items";

/**
 * The labels the list's filter bar shows for the given field ids — for a backend answer that names the
 * fields it left out (see `OutgoingInvoiceEntityRest.comparisonIgnoredFields`). The three history fields
 * share one label, so it occurs once. Reads `listMeta` from the cache the list already filled.
 */
export function useFilterFieldLabels(
  entity: string,
  fields: readonly string[] | null | undefined
): string[] {
  const t = useTranslations();
  const meta = useListMeta(entity);
  return useMemo(() => {
    const elements = filterElementsOf(meta.data);
    const labels = (fields ?? []).map((field) =>
      filterFieldLabel(field, elements, t("filter.history"))
    );
    return [...new Set(labels)];
  }, [fields, meta.data, t]);
}
