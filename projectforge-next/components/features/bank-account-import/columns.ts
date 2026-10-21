import type { ImportColumn } from "@/components/shared/import/import-types";

/**
 * The preview columns of the import of bank account transactions, those of the React import page. A file's
 * transaction is paired with a stored one of the same day, so the date never differs; every other column may
 * change against the stored transaction, hence `diff`.
 */
export const BANK_ACCOUNT_IMPORT_COLUMNS: ImportColumn[] = [
  {
    field: "date",
    headerKey: "plugins.banking.account.record.date",
    kind: "date",
    width: 110,
  },
  {
    field: "valueDate",
    headerKey: "plugins.banking.account.record.valueDate",
    kind: "date",
    diff: true,
    width: 110,
  },
  {
    field: "amount",
    headerKey: "plugins.banking.account.record.amount",
    kind: "currency",
    diff: true,
    width: 120,
  },
  {
    field: "receiverSender",
    headerKey: "plugins.banking.account.record.receiverSender",
    kind: "text",
    diff: true,
    width: 200,
  },
  {
    field: "iban",
    headerKey: "plugins.banking.account.iban",
    kind: "text",
    diff: true,
    width: 190,
  },
  {
    field: "bic",
    headerKey: "plugins.banking.account.bic",
    kind: "text",
    diff: true,
    width: 110,
  },
  {
    field: "subject",
    headerKey: "plugins.banking.account.record.subject",
    kind: "text",
    diff: true,
    width: 300,
  },
  {
    field: "collectionReference",
    headerKey: "plugins.banking.account.record.collectionReference",
    kind: "text",
    diff: true,
    width: 160,
  },
  {
    field: "debteeId",
    headerKey: "plugins.banking.account.record.debteeId",
    kind: "text",
    diff: true,
    width: 160,
  },
  {
    field: "mandateReference",
    headerKey: "plugins.banking.account.record.mandateReference",
    kind: "text",
    diff: true,
    width: 160,
  },
  {
    field: "customerReference",
    headerKey: "plugins.banking.account.record.customerReference",
    kind: "text",
    diff: true,
    width: 160,
  },
  {
    field: "currency",
    headerKey: "plugins.banking.account.record.currency",
    kind: "text",
    diff: true,
    width: 90,
  },
  {
    field: "type",
    headerKey: "plugins.banking.account.record.type",
    kind: "text",
    diff: true,
    width: 140,
  },
  {
    field: "info",
    headerKey: "plugins.banking.account.record.info",
    kind: "text",
    diff: true,
    width: 160,
  },
];
