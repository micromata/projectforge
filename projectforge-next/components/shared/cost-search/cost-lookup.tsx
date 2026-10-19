"use client";

import { useId, useState } from "react";
import { useTranslations } from "next-intl";
import { useQuery } from "@tanstack/react-query";
import { CopyableValue } from "@/components/shared/copyable-value";
import { EntityAutocomplete } from "@/components/shared/entity-autocomplete";
import { Label } from "@/components/ui/label";
import {
  COST_SEARCH_QUERY_KEY,
  fetchCostDetails,
  type CostKind,
} from "@/lib/rs/cost-search";

interface Props {
  kind: CostKind;
  /** "Cost 1" / "Cost 2", the label of the picker. */
  label: string;
  className?: string;
}

/**
 * One half of the cost search, as two rows of CostSearchConverter's grid: the label beside a cost 1 or
 * cost 2 picker, and below the picker the number and long name of the unit picked last, each copyable.
 *
 * The picker itself stays empty (`value` null), as on the React page: the result is shown below it, and
 * the next unit can be typed straight away instead of first clearing the previous one. Inactive units
 * are offered via the picker's own "show inactive" checkbox (useShowInactive), remembered per entity.
 */
export function CostLookup({ kind, label, className }: Props) {
  const t = useTranslations();
  const inputId = useId();
  const [pickedId, setPickedId] = useState<number | null>(null);
  const details = useQuery({
    queryKey: [...COST_SEARCH_QUERY_KEY, kind, pickedId],
    queryFn: ({ signal }) => fetchCostDetails(kind, pickedId!, signal),
    enabled: pickedId != null,
  });
  const number = details.data ? String(details.data.number) : null;
  return (
    <>
      <Label htmlFor={inputId} className={className}>
        {label}
      </Label>
      <EntityAutocomplete
        id={inputId}
        url={`${kind}/autosearch?search=:search`}
        value={null}
        onChange={(picked) => picked && setPickedId(picked.id)}
        className={className}
        // The long cost 2 names don't fit a narrow panel's width.
        popoverClassName="w-[min(36rem,90vw)]"
      />
      <div className="col-start-2 flex gap-1.5">
        <CopyableValue
          value={number}
          label={`${label}: ${t("fibu.kostSearch.number")}`}
          className="w-28 shrink-0"
        />
        <CopyableValue
          value={details.data?.name}
          label={`${label}: ${t("name")}`}
          className="min-w-0 flex-1"
        />
      </div>
    </>
  );
}
