import { describe, expect, it } from "vitest";
import { leafKeyOf } from "@/lib/leaf-key";
import { invoiceStatisticsEntries } from "@/components/features/invoice/invoice-statistics";
import { orderStatisticsEntries } from "@/components/features/order/order-statistics";
import { LOCALES, MESSAGES } from "./config";

/**
 * The keys a renderer hands to `t()` without a metadata field behind them, against the real catalogs.
 *
 * A key that is a text *and* a namespace in `I18nResources.properties` is exported as `<key>._`, and
 * asking next-intl for the bare one throws `INSUFFICIENT_PATH` at runtime — which no typecheck sees and
 * no unit test with a catalogue of its own would notice, since whether a key collides is a property of
 * the bundle. Hence this runs over the generated catalogs as they are.
 *
 * Column labels are not listed here: they go through `labelKeyFor`, which is covered by
 * `lib/page-def/define-page.test.ts`.
 */
const FILLED_STATISTICS = {
  brutto: 1190,
  bruttoWithDiscount: 1150,
  netto: 1000,
  open: 400,
  overdue: 200,
  discount: 40,
  paymentTargetAverage: 30,
  actualPaymentTargetAverage: 42,
};

/** Every optional entry present, so no key of a statistics line escapes the check. */
const KEYS = [
  ...invoiceStatisticsEntries(FILLED_STATISTICS).map((e) => e.labelKey),
  // Every counter set: an order line is dropped while its counter is 0.
  ...orderStatisticsEntries({
    counter: 1,
    netSum: 1000,
    akquiseSum: 500,
    counterAkquise: 1,
    commissionedSum: 400,
    counterCommissioned: 1,
    invoicedSum: 200,
    counterInvoiced: 1,
    notYetInvoicedSum: 150,
    counterNotYetInvoiced: 1,
    toBeInvoicedSum: 300,
    counterToBeInvoiced: 1,
  }).map((e) => e.labelKey),
  // The list legends and the export buttons, which name their keys in place.
  "fibu.rechnung.filter.ueberfaellig",
  "fibu.rechnung.offen",
  "fibu.rechnung.kostExcelExport",
  "exportAsXls",
];

function resolve(
  messages: Record<string, unknown>,
  key: string
): unknown | undefined {
  let node: unknown = messages;
  for (const part of key.split(".")) {
    if (typeof node !== "object" || node === null) return undefined;
    node = (node as Record<string, unknown>)[part];
  }
  return node;
}

describe.each(LOCALES)("catalogue %s", (locale) => {
  const messages = MESSAGES[locale];
  const hasMessage = (key: string) =>
    typeof resolve(messages, key) === "string";

  it.each(KEYS)("resolves %s to a text", (key) => {
    const resolved = resolve(messages, leafKeyOf(key, hasMessage));
    expect(typeof resolved, `${key} in ${locale}`).toBe("string");
  });
});
