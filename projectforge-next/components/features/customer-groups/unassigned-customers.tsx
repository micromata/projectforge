"use client";

import { useMemo } from "react";
import { keepPreviousData, useQuery } from "@tanstack/react-query";
import { useStore } from "@tanstack/react-form";
import { useTranslations } from "next-intl";
import { FreeTextBadge } from "@/components/shared/free-text-badge";
import { useEntityEditForm } from "@/components/shared/form/form-context";
import { GroupBadge } from "@/components/shared/group-badge";
import { useDebouncedValue } from "@/hooks/use-debounced-value";
import { cn } from "@/lib/utils";
import {
  fetchUnassignedCustomers,
  type UnassignedEntry,
} from "@/lib/rs/customer-groups";
import type { CustomerGroupsValues } from "./types";
import { toPayload } from "./values";

/** As the live validation: a name typed letter by letter is asked about once. */
const DELAY_MILLIS = 500;

/**
 * What the business units leave out — the groups in none, and the customers and free texts in no group —
 * so a customer forgotten is seen while editing. Evaluated by the server on the unsaved values
 * (`POST /rs/customerGroups/unassigned`), over the orders and invoices of the last five years. Grouped by
 * the year of last use, as the server sorts them (most recent first, then by name).
 */
export function UnassignedCustomers() {
  const t = useTranslations();
  const form = useEntityEditForm();
  const values = useStore(
    form.store,
    (s: unknown) => (s as { values: CustomerGroupsValues }).values
  );
  const debounced = useDebouncedValue(values, DELAY_MILLIS);
  const payload = useMemo(() => toPayload(debounced), [debounced]);
  const { data } = useQuery({
    queryKey: ["customerGroups", "unassigned", payload],
    queryFn: ({ signal }) => fetchUnassignedCustomers(payload, signal),
    placeholderData: keepPreviousData,
  });
  if (!data) return null;
  const entries = data.entries ?? [];
  const years = groupByYear(entries);
  return (
    <section className="flex flex-col gap-2">
      <h2 className="text-sm font-semibold">
        {t("fibu.businessUnits.unassigned", { arg0: entries.length })}
      </h2>
      <p className="text-xs text-muted-foreground">
        {t("fibu.businessUnits.unassignedHint")}
      </p>
      {entries.length === 0 ? (
        <p className="text-sm text-muted-foreground">
          {t("fibu.businessUnits.unassignedNone")}
        </p>
      ) : (
        <div className="flex flex-col gap-2 text-xs">
          {years.map(([year, items]) => (
            <section key={year}>
              <h3 className="pb-0.5 text-muted-foreground">{year}</h3>
              <ul className="grid gap-x-4 sm:grid-cols-2">
                {items.map((entry) => (
                  <UnassignedItem
                    key={`${entry.kind}:${entry.name}`}
                    entry={entry}
                  />
                ))}
              </ul>
            </section>
          ))}
        </div>
      )}
    </section>
  );
}

/** Consecutive entries of the same year, in the server's order. */
function groupByYear(entries: UnassignedEntry[]) {
  const years: [number, UnassignedEntry[]][] = [];
  for (const entry of entries) {
    const last = years.at(-1);
    if (last?.[0] === entry.year) last[1].push(entry);
    else years.push([entry.year, [entry]]);
  }
  return years;
}

function UnassignedItem({ entry }: { entry: UnassignedEntry }) {
  return (
    <li
      className={cn(
        "flex min-w-0 items-center gap-1.5",
        // Italic, as the free-text customers are set apart in the checklists too.
        entry.kind === "FREE_TEXT" && "italic"
      )}
    >
      <span className="truncate">{entry.name}</span>
      {entry.kind === "GROUP" && <GroupBadge />}
      {entry.kind === "FREE_TEXT" && <FreeTextBadge />}
    </li>
  );
}
