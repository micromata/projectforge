import type { ImportColumn } from "@/components/shared/import/import-types";

/** Position-based files (header + positions) — the backend's default when the flag is absent. */
const positionBased = (meta: Record<string, unknown>): boolean =>
  meta.isPositionBasedImport !== false;

/** Header-only files — positions are calculated, so the payment/bank fields take their place. */
const headerOnly = (meta: Record<string, unknown>): boolean =>
  meta.isPositionBasedImport === false;

/**
 * The preview columns of the creditor-invoice import, in the order and with the gating of the legacy
 * `IncomingInvoicePosImportPageRest`: the position number, cost centres and cost accounts only for a
 * position-based file; the payment date, amount, type and bank details only for a header-only one. The
 * diffed columns are those the reconcile can change against an existing invoice.
 */
export const CREDITOR_INVOICE_IMPORT_COLUMNS: ImportColumn[] = [
  {
    field: "positionNummer",
    headerKey: "label.position.short",
    kind: "number",
    diff: true,
    width: 70,
    showIf: positionBased,
  },
  {
    field: "referenz",
    headerKey: "fibu.common.reference",
    kind: "text",
    width: 130,
  },
  {
    field: "kreditor",
    headerKey: "fibu.common.creditor",
    kind: "text",
    width: 180,
  },
  { field: "datum", headerKey: "fibu.rechnung.datum", kind: "date" },
  { field: "grossSum", headerKey: "fibu.common.brutto", kind: "currency" },
  {
    field: "currency",
    headerKey: "fibu.rechnung.currency",
    kind: "text",
    width: 80,
  },
  {
    field: "betreff",
    headerKey: "fibu.rechnung.betreff",
    kind: "text",
    width: 180,
  },
  {
    field: "konto.nummer",
    headerKey: "fibu.konto",
    kind: "text",
    diff: true,
    width: 100,
  },
  {
    field: "kost1.description",
    headerKey: "fibu.kost1",
    kind: "text",
    diff: true,
    width: 150,
    showIf: positionBased,
  },
  {
    field: "kost2.description",
    headerKey: "fibu.kost2",
    kind: "text",
    diff: true,
    width: 150,
    showIf: positionBased,
  },
  {
    field: "faelligkeit",
    headerKey: "fibu.rechnung.faelligkeit",
    kind: "date",
  },
  {
    field: "bezahlDatum",
    headerKey: "fibu.rechnung.bezahlDatum",
    kind: "date",
    showIf: headerOnly,
  },
  {
    field: "zahlBetrag",
    headerKey: "fibu.rechnung.zahlBetrag",
    kind: "currency",
    showIf: headerOnly,
  },
  {
    field: "paymentTypeAsString",
    headerKey: "fibu.payment.type",
    kind: "text",
    diff: true,
    width: 150,
    showIf: headerOnly,
  },
  {
    field: "taxRate",
    headerKey: "fibu.rechnung.mehrwertSteuerSatz",
    kind: "number",
  },
  { field: "bemerkung", headerKey: "comment", kind: "text", width: 150 },
  {
    field: "customernr",
    headerKey: "fibu.rechnung.customernr",
    kind: "text",
    width: 150,
  },
  {
    field: "discountPercent",
    headerKey: "fibu.rechnung.discountPercent",
    kind: "percentage",
  },
  {
    field: "discountMaturity",
    headerKey: "fibu.rechnung.discountMaturity",
    kind: "date",
  },
  {
    field: "iban",
    headerKey: "fibu.rechnung.iban",
    kind: "text",
    showIf: headerOnly,
  },
  {
    field: "bic",
    headerKey: "fibu.rechnung.bic",
    kind: "text",
    showIf: headerOnly,
  },
];
