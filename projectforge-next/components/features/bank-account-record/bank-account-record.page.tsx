import { BANK_ACCOUNT_RECORD_METADATA } from "@/lib/metadata/bank-account-record.generated";
import { definePage } from "@/lib/page-def/define-page";
import { BankAccountRecordAccountField } from "./bank-account-record-account-field";
import {
  bankAccountRecordSchema,
  BANK_ACCOUNT_RECORD_FIELDS,
  type BankAccountRecordValues,
} from "./bank-account-record-schema";
import { BankAccountRecordStatisticsLine } from "./bank-account-record-statistics-line";
import {
  accountLabel,
  emptyBankAccountRecordValues,
  toFormValues,
} from "./bank-account-record-values";
import type {
  BankAccountRecordDetail,
  BankAccountRecordListRow,
  BankAccountRecordStatistics,
} from "./types";

/** React Query key of the list, so a write from the edit page refreshes it. */
export const BANK_ACCOUNT_RECORD_LIST_QUERY_KEY = [
  "bankAccountRecord",
] as const;

/**
 * The transactions of the bank accounts (banking plugin) — list and edit — as data (see lib/page-def/types.ts).
 *
 * Replaces the removed React pages (`BankAccountRecordPagesRest`), which it follows: the list's columns, its
 * filters (period, accounts, doublets, checksum errors — declared by the backend) and the sums of the result set;
 * an income reads green. The form has every field of a transaction, the account fixed once it is stored.
 */
export const BANK_ACCOUNT_RECORD_PAGE = definePage<
  BankAccountRecordListRow,
  BankAccountRecordValues,
  BankAccountRecordDetail,
  typeof BANK_ACCOUNT_RECORD_METADATA
>({
  entity: "bankAccountRecord",
  metadata: BANK_ACCOUNT_RECORD_METADATA,
  route: "/bankAccountRecord",
  queryKey: BANK_ACCOUNT_RECORD_LIST_QUERY_KEY,
  // Where the entry sits in the main menu: Finance, beside the bank accounts (BankingPlugin).
  categoryKey: "menu.fibu._",
  titleKey: "plugins.banking.account.record.title.list",
  defaultSort: { id: "date", desc: true },
  columns: [
    {
      id: "accountIban",
      labelKey: "plugins.banking.account.record.accountIban",
      accessor: (row) => row.bankAccount?.iban ?? "",
      size: 200,
    },
    {
      id: "accountName",
      labelKey: "plugins.banking.account.record.accountName",
      accessor: (row) => accountLabel(row.bankAccount),
      size: 160,
    },
    { name: "date", size: 110 },
    { name: "amount", size: 120 },
    { name: "subject", size: 320, wrap: true },
    { name: "receiverSender", size: 220, wrap: true },
    { name: "iban", size: 200 },
    { name: "bic", size: 120 },
    { name: "comment", size: 200, wrap: true },
    { name: "valueDate", size: 110, hiddenByDefault: true },
    { name: "type", size: 140, hiddenByDefault: true },
  ],
  legend: [
    {
      className: "row-red",
      labelKey: "plugins.banking.account.record.checksumErrors",
    },
    {
      className: "row-green",
      labelKey: "plugins.banking.account.record.income",
    },
  ],
  rowClassName: (row) => {
    if (row.checksumError) return "row-red";
    if ((row.amount ?? 0) >= 0) return "row-green";
    return undefined;
  },
  // The sums of the whole result set (`BankAccountRecordEntityRest.Statistics`); the cast is where the untyped
  // `ResultSet.statistics` becomes what the backend sends.
  statistics: ({ statistics, isFetching }) => (
    <BankAccountRecordStatisticsLine
      statistics={statistics as BankAccountRecordStatistics | undefined}
      isFetching={isFetching}
    />
  ),
  edit: {
    schema: bankAccountRecordSchema,
    fieldNames: BANK_ACCOUNT_RECORD_FIELDS,
    defaultValues: emptyBankAccountRecordValues,
    toFormValues,
    // Who paid or was paid — the transaction as one finds it on the statement (the date is beside it in the list).
    title: (record) => record.receiverSender ?? record.subject ?? "",
    newTitleKey: "plugins.banking.account.record.title.add",
    savedMessageKey: "message.successfullChanged",
    clone: true,
    sections: [
      {
        id: "general",
        titleKey: "plugins.banking.account.record.transaction",
        fields: [
          { custom: BankAccountRecordAccountField, span: 2 },
          { name: "date", startsRow: true },
          { name: "valueDate" },
          { name: "amount", startsRow: true },
          { name: "currency" },
          { name: "receiverSender", span: 2, startsRow: true },
          { name: "iban" },
          { name: "bic" },
          { name: "subject", span: 3, rows: 2 },
          { name: "comment", span: 3, rows: 2 },
          { name: "info" },
          { name: "type" },
        ],
      },
      {
        id: "references",
        titleKey: "plugins.banking.account.record.references",
        fields: [
          { name: "debteeId" },
          { name: "customerReference" },
          { name: "mandateReference" },
          { name: "collectionReference" },
        ],
      },
    ],
  },
});
