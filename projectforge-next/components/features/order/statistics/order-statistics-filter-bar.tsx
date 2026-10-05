"use client";

import { useTranslations } from "next-intl";
import { FilterPills } from "@/components/data-table/filter-pills";
import { FavoritesMenu } from "@/components/shared/favorites/favorites-menu";
import type { FilterElement } from "@/lib/rs/types";
import type { useOrderStatisticsFilter } from "./use-order-statistics-filter";

/**
 * The filter row of the order statistics page: business units, customers and projects as pills (their
 * choices come from the order book's `*FilterValues` endpoints, so only those of visible orders), and
 * the page's own favorites at the end.
 */
export function OrderStatisticsFilterBar({
  elements,
  state,
}: {
  elements: FilterElement[];
  state: ReturnType<typeof useOrderStatisticsFilter>;
}) {
  const t = useTranslations();
  return (
    <FilterPills
      elements={elements}
      values={state.values}
      onChange={state.setValues}
      trailing={
        <FavoritesMenu
          favorites={state.favorites}
          currentId={state.currentId}
          isModified={state.isModified}
          onSelect={state.select}
          onCreate={state.create}
          onRename={state.rename}
          onUpdate={state.update}
          onDelete={state.remove}
          label={t("favorites._")}
          showLabel
        />
      }
    />
  );
}
