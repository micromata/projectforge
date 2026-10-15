"use client";

import { useTranslations } from "next-intl";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import { useFilterFieldLabels } from "@/components/shared/chart/use-filter-field-labels";
import { useFormatContext } from "@/hooks/use-format";
import { formatDateRange } from "@/lib/format";

/**
 * The "*" after the "Vorjahr" row's label: the window it compares against — "Vorjahr" over a year-to-date
 * range is a partial year, not a whole one — and the list's criteria the year-earlier figures leave out
 * (the invoice's current state: paid, status, ...; see `OutgoingInvoiceEntityRest.comparisonFilter`).
 */
export function PreviousYearFootnote({
  previousPeriod,
  entity,
  ignoredFilterFields,
}: {
  previousPeriod: { from: string; to: string };
  /** The list whose filter labels name the left-out fields; without one they aren't named. */
  entity?: string;
  ignoredFilterFields?: readonly string[] | null;
}) {
  return entity && ignoredFilterFields?.length ? (
    <WithIgnoredFields
      previousPeriod={previousPeriod}
      entity={entity}
      ignoredFilterFields={ignoredFilterFields}
    />
  ) : (
    <Footnote previousPeriod={previousPeriod} />
  );
}

function WithIgnoredFields({
  previousPeriod,
  entity,
  ignoredFilterFields,
}: {
  previousPeriod: { from: string; to: string };
  entity: string;
  ignoredFilterFields: readonly string[];
}) {
  const t = useTranslations("fibu.rechnung.statistics");
  const labels = useFilterFieldLabels(entity, ignoredFilterFields);
  return (
    <Footnote
      previousPeriod={previousPeriod}
      remark={t("previousYearWithoutFilters", { arg0: labels.join(", ") })}
    />
  );
}

function Footnote({
  previousPeriod,
  remark,
}: {
  previousPeriod: { from: string; to: string };
  remark?: string;
}) {
  const t = useTranslations("fibu.rechnung.statistics");
  const format = useFormatContext();
  const period = formatDateRange(
    previousPeriod.from,
    previousPeriod.to,
    format
  );
  return (
    <HintTooltip
      openOnTap
      title={t("previousYearPeriod")}
      text={remark ? `${period}\n\n${remark}` : period}
      plain
    >
      {/* A footnote marker, not a control: the caret in the corner already toggles the row. */}
      <sup className="ml-0.5 cursor-help">*</sup>
    </HintTooltip>
  );
}
