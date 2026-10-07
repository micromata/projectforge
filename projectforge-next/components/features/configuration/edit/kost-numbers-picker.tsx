"use client";

import { useQueries } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import type { EntityRef } from "@/components/shared/entity-autocomplete";
import { EntityMultiAutocomplete } from "@/components/shared/entity-multi-autocomplete";
import { fetchAutoCompletion } from "@/lib/rs/dynamic";
import { kostNumberOf } from "../lanes-and-planes-config";

/** As the lookups of the pickers (see use-entity-lookup.ts). */
const STALE_MS = 60_000;

/**
 * Kost1 or Kost2 (by [kind]) picked by searching, stored as their formatted numbers (what the parameter holds and the backend
 * validates). The names of stored numbers are looked up one by one (the search finds a dotted number
 * exactly, inactive ones included); until then, or for an unknown number, the chip shows the number.
 */
export function KostNumbersPicker({
  kind,
  value,
  onChange,
  ariaLabel,
}: {
  kind: "cost1" | "cost2";
  value: string[];
  onChange: (value: string[]) => void;
  ariaLabel: string;
}) {
  const t = useTranslations("lanesAndPlanes.config");
  const url = `${kind}/autosearch?search=:search`;
  const lookups = useQueries({
    queries: value.map((number) => ({
      queryKey: ["lanesAndPlanes", kind, number],
      queryFn: ({ signal }: { signal: AbortSignal }) =>
        fetchAutoCompletion<EntityRef>(url, number, undefined, signal),
      staleTime: STALE_MS,
    })),
  });
  const refs: EntityRef[] = value.map((number, index) => {
    const lookup = lookups[index];
    const hit = lookup?.data?.find(
      (entry) => kostNumberOf(entry.displayName) === number
    );
    if (hit) return { id: hit.id, displayName: hit.displayName };
    // No entity id yet: a negative one only keys the chip (and can't collide with a real id).
    const displayName = lookup?.isSuccess
      ? `${number} (${t("kostNotFound")})`
      : number;
    return { id: -(index + 1), displayName };
  });
  return (
    <EntityMultiAutocomplete
      url={url}
      aria-label={ariaLabel}
      value={refs}
      sorted
      removeLabel={(entry) => t("remove", { arg0: entry.displayName })}
      onChange={(picked) => {
        const numbers: string[] = [];
        picked.forEach((entry) => {
          // A stored chip keeps its number even while its name is unknown.
          const number =
            value[refs.indexOf(entry)] ?? kostNumberOf(entry.displayName);
          if (number && !numbers.includes(number)) numbers.push(number);
        });
        onChange(numbers);
      }}
    />
  );
}
