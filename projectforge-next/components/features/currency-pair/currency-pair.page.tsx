import { CURRENCY_PAIR_METADATA } from "@/lib/metadata/currency-pair.generated";
import { definePage } from "@/lib/page-def/define-page";
import { CurrencyPairEditIntro } from "./edit/currency-pair-edit-intro";
import { CurrencyPairRatesSection } from "./edit/currency-pair-rates-section";
import { CurrencyPairRateCell } from "./currency-pair-rate-cell";
import { CURRENCY_PAIR_LIST_QUERY_KEY } from "./currency-pair-query-key";
import {
  currencyPairSchema,
  CURRENCY_PAIR_FIELDS,
  type CurrencyPairValues,
} from "./currency-pair-schema";
import { emptyCurrencyPairValues, toFormValues } from "./currency-pair-values";
import type { CurrencyPairDetail, CurrencyPairListRow } from "./types";

/**
 * The currency pairs (`CurrencyPairEntityRest`), migrated from the React UILayout page: the list with the rate in
 * force today, and the form with the conversion rates of a stored pair, each valid from its date.
 */
export const CURRENCY_PAIR_PAGE = definePage<
  CurrencyPairListRow,
  CurrencyPairValues,
  CurrencyPairDetail,
  typeof CURRENCY_PAIR_METADATA
>({
  entity: "currencyPair",
  metadata: CURRENCY_PAIR_METADATA,
  route: "/currencyPair",
  queryKey: CURRENCY_PAIR_LIST_QUERY_KEY,
  categoryKey: "menu.fibu._",
  titleKey: "fibu.currencyPair.title.list",
  defaultSort: { id: "sourceCurrency" },
  // The columns of the legacy list, in its order. The current rate is computed (transient), hence not sortable.
  columns: [
    { name: "sourceCurrency", size: 140, className: "font-semibold" },
    { name: "targetCurrency", size: 140, className: "font-semibold" },
    {
      id: "currentRateDate",
      labelKey: "attr.validSince",
      accessor: (row) => row.currentRateDate ?? null,
      dataType: "DATE",
      sortable: false,
      size: 140,
    },
    {
      id: "currentRate",
      labelKey: "fibu.currencyConversion.currentRate",
      accessor: (row) => row.currentRate ?? null,
      align: "right",
      cell: ({ row }) => (
        <CurrencyPairRateCell value={row.original.currentRate} />
      ),
      sortable: false,
      size: 200,
    },
    {
      id: "currentInverseRate",
      labelKey: "fibu.currencyConversion.currentInverseRate",
      accessor: (row) => row.currentInverseRate ?? null,
      align: "right",
      cell: ({ row }) => (
        <CurrencyPairRateCell value={row.original.currentInverseRate} />
      ),
      sortable: false,
      size: 200,
    },
    { name: "comment", size: 300, wrap: true },
    { name: "created", size: 130, hiddenByDefault: true },
    { name: "lastUpdate", size: 130, hiddenByDefault: true },
  ],
  edit: {
    schema: currencyPairSchema,
    fieldNames: CURRENCY_PAIR_FIELDS,
    defaultValues: emptyCurrencyPairValues,
    toFormValues,
    title: (pair) =>
      pair.displayName ??
      `${pair.sourceCurrency ?? ""} → ${pair.targetCurrency ?? ""}`,
    newTitleKey: "fibu.currencyPair.title.add",
    savedMessageKey: "message.successfullChanged",
    editIntro: CurrencyPairEditIntro,
    sections: [
      {
        id: "general",
        titleKey: "fibu.currencyPair",
        fields: [
          { name: "sourceCurrency" },
          { name: "targetCurrency" },
          { name: "comment", span: 3, rows: 4 },
        ],
      },
      {
        id: "rates",
        titleKey: "fibu.currencyConversion.rates",
        // Only for a stored pair: the rates need its id.
        visible: ({ data }) => data?.id != null,
        render: ({ id }) =>
          id == null ? null : <CurrencyPairRatesSection id={id} />,
      },
    ],
  },
});
