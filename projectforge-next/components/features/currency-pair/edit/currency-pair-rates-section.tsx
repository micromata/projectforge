"use client";

import { useQueryClient } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import { MarkdownText } from "@/components/shared/markdown-text";
import { ValidityEntriesSection } from "@/components/shared/validity-entries/validity-entries-section";
import type {
  ValidityEntriesAdapter,
  ValidityValueField,
} from "@/components/shared/validity-entries/types";
import { useEntityDetail } from "@/hooks/use-entity-detail";
import { useFormatContext } from "@/hooks/use-format";
import { historyQueryKey } from "@/hooks/use-history";
import {
  deleteRate,
  fetchOnlineRates,
  insertRate,
  updateRate,
} from "@/lib/rs/currency-pair-rate";
import { todayOf } from "@/lib/user-zone";
import { RATE_FRACTION_DIGITS } from "../currency-pair-rate-cell";
import { CURRENCY_PAIR_LIST_QUERY_KEY } from "../currency-pair-query-key";
import type { CurrencyConversionRate, CurrencyPairDetail } from "../types";

const ADAPTER: ValidityEntriesAdapter<CurrencyConversionRate> = {
  insert: insertRate,
  update: updateRate,
  remove: deleteRate,
};

/**
 * The conversion rates of a stored currency pair, each valid from its date. Written through
 * `currencyPair/rate/...` right away, not with the form (see ValidityEntriesSection). A new rate is proposed
 * from the online source for its date (`currencyPair/rate/onlineRates`). The answer of a write replaces the rates
 * of the cached pair (no refetch: it already is what the backend has); the list, whose current rate follows the
 * rates, and the history are refreshed.
 */
export function CurrencyPairRatesSection({ id }: { id: number }) {
  const t = useTranslations();
  const queryClient = useQueryClient();
  const ctx = useFormatContext();
  const pair = useEntityDetail<CurrencyPairDetail>("currencyPair", id).data;
  // The stored codes: the rates belong to the pair as saved, not to an unsaved change in the form.
  const source = pair?.sourceCurrency ?? "";
  const target = pair?.targetCurrency ?? "";
  // As the legacy dialog: "1 EUR = [0.92] USD" and back.
  const valueFields: ValidityValueField[] = [
    {
      name: "conversionRate",
      labelKey: "fibu.currencyConversion.conversionRate",
      label: source ? `1 ${source} =` : undefined,
      suffix: target || undefined,
      kind: "decimal",
      maxFractionDigits: RATE_FRACTION_DIGITS,
      required: true,
    },
    {
      name: "inverseConversionRate",
      labelKey: "fibu.currencyConversion.inverseConversionRate",
      label: target ? `1 ${target} =` : undefined,
      suffix: source || undefined,
      kind: "decimal",
      maxFractionDigits: RATE_FRACTION_DIGITS,
      required: true,
    },
  ];

  return (
    <div className="flex flex-col gap-3">
      <MarkdownText
        text={t("fibu.currencyPair.onlineRates.info")}
        className="text-muted-foreground text-sm"
      />
      <ValidityEntriesSection<CurrencyConversionRate>
        entries={pair?.rateEntries ?? []}
        dateField="validFrom"
        valueFields={valueFields}
        dialogTitle={t("fibu.currencyConversion.conversionRate")}
        // Today, as the legacy dialog did: the rates proposed online are today's then.
        newEntry={() => ({
          id: null,
          validFrom: todayOf(ctx),
          currencyPairId: id,
        })}
        adapter={ADAPTER}
        // `null` is "not asked" and reads as allowed (lib/rs/entity-access.ts); the endpoints check anyway.
        readOnly={pair?.writeAccess === false}
        prefill={(_entry, date) => fetchOnlineRates(id, date)}
        onChanged={(entries) => {
          queryClient.setQueryData<CurrencyPairDetail>(
            ["currencyPair", id],
            (old) => (old ? { ...old, rateEntries: entries } : old)
          );
          // The list shows the rate in force today. Not the pair itself: the answer above already is what the
          // backend has now.
          void queryClient.invalidateQueries({
            queryKey: CURRENCY_PAIR_LIST_QUERY_KEY,
            predicate: (query) => query.queryKey[1] !== id,
          });
          void queryClient.invalidateQueries({
            queryKey: historyQueryKey("currencyPair", id),
          });
        }}
      />
    </div>
  );
}
