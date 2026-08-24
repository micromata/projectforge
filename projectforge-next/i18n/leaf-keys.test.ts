/// <reference types="vite/client" />
import { describe, expect, it } from "vitest";
import { leafKeyOf } from "@/lib/leaf-key";
import { invoiceStatisticsEntries } from "@/components/shared/invoice/invoice-statistics";
import { orderStatisticsEntries } from "@/components/features/order/order-statistics";
import { BOOK_PAGE } from "@/components/features/book/book.page";
import { COST1_PAGE } from "@/components/features/cost1/cost1.page";
import { INVOICE_PAGE } from "@/components/features/invoice/invoice.page";
import { ORDER_PAGE } from "@/components/features/order/order.page";
import { LOCALES, MESSAGES } from "./config";

/**
 * Every generated entity metadata module, by file — glob rather than a list, so an entity added later is
 * checked without anybody remembering to name it here.
 */
const ENTITY_MODULES: Record<
  string,
  Record<string, unknown>
> = import.meta.glob("../lib/metadata/*.generated.ts", {
  eager: true,
}) as Record<string, Record<string, unknown>>;

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
  // The selection mode's own texts, composed in place (see SelectionBar and ListToolbar). Both
  // shortcut keys are a text *and* a namespace, which is exactly what leafKeyOf is for.
  "tooltip.shortcut.addEntry",
  "tooltip.shortcut.addEntry.title",
  "tooltip.shortcut.selectAll",
  "tooltip.shortcut.selectAll.title",
  // The shortcut of the default button, composed by useSubmitShortcutHint.
  "tooltip.shortcut.submitForm",
  "tooltip.shortcut.submitForm.title",
  "multiselection.button",
  "multiselection.aggrid.selection.info.title",
  "multiselection.aggrid.selection.info.message",
  "selectAll",
  "deselectAll",
  "cancel",
  // The account of an invoice, whose key is a text *and* the parent of the whole address block.
  "fibu.konto",
  ...pageKeys(),
];

/**
 * Every key a page declaration names in plain text — its title, its menu parent, the heading of each
 * section and of each tab beside the form.
 *
 * These are the ones no other check reaches: a field's label goes through `labelKeyFor` (covered by
 * `lib/page-def/define-page.test.ts`), but a section title is written into the declaration and rendered
 * by `DeclaredSection`/`entityTabs`, so nothing but the catalogue can say whether it is a text at all.
 * A section named after its entity (`fibu.rechnung`, `fibu.auftrag`) is precisely the colliding case.
 */
function pageKeys(): string[] {
  const pages = [BOOK_PAGE, COST1_PAGE, INVOICE_PAGE, ORDER_PAGE];
  return pages.flatMap((page) => [
    page.categoryKey,
    page.titleKey,
    ...("edit" in page && page.edit
      ? [
          page.edit.newTitleKey,
          page.edit.savedMessageKey,
          ...page.edit.sections.flatMap((section) => [
            section.titleKey,
            ...(section.tabTitleKey ? [section.tabTitleKey] : []),
          ]),
          ...(page.edit.extraTabs ?? []).map((tab) => tab.labelKey),
        ]
      : []),
  ]);
}

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

  /**
   * No field label may resolve to a *subtree*, over every generated entity at once.
   *
   * The narrower sibling of the checks above, and the one that catches what they cannot: an `i18nKey`
   * pointing at a namespace that holds no text of its own. `RechnungsPositionDO.periodOfPerformanceType`
   * named `fibu.periodOfPerformance.type`, which exists only as the parent of the enum's two values — so
   * every position row threw `INSUFFICIENT_PATH`. Nothing else sees it: the key resolves in the bundle
   * check on the Kotlin side, which counts a namespace of known keys as known, and `labelKeyFor` finds
   * no `._` leaf to fall back to.
   *
   * A key that is **absent** is deliberately not failed here: the plugins keep resource bundles the
   * generator does not read (`plugins.banking.*`, `plugins.todo.*`), and `ContractDO` declares a literal
   * prefix as its key. next-intl renders those as the key itself — visible, but not a crash.
   */
  it("resolves every field label of every entity to a text, never a subtree", () => {
    const subtrees = Object.entries(ENTITY_MODULES).flatMap(([file, module]) =>
      Object.values(module).flatMap((exported) => {
        const metadata = exported as {
          fields?: Record<string, { i18nKey?: string }>;
        };
        return Object.entries(metadata.fields ?? {}).flatMap(
          ([name, field]) => {
            if (!field.i18nKey) return [];
            const resolved = resolve(
              messages,
              leafKeyOf(field.i18nKey, hasMessage)
            );
            if (resolved === undefined || typeof resolved === "string")
              return [];
            return [`${file}: ${name} → ${field.i18nKey}`];
          }
        );
      })
    );
    expect(subtrees).toEqual([]);
  });
});
