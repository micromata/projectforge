import { BANK_ACCOUNT_METADATA } from "@/lib/metadata/bank-account.generated";
import { definePage } from "@/lib/page-def/define-page";
import {
  BankAccountFullAccessGroupsField,
  BankAccountFullAccessUsersField,
  BankAccountReadonlyAccessGroupsField,
  BankAccountReadonlyAccessUsersField,
} from "./bank-account-access-fields";
import { BankAccountImportSettingsHelp } from "./bank-account-import-settings-help";
import {
  bankAccountImportHref,
  bankAccountRecordsHref,
} from "./bank-account-links";
import { BankAccountListActions } from "./bank-account-list-actions";
import {
  bankAccountSchema,
  BANK_ACCOUNT_ARRAY_FIELDS,
  BANK_ACCOUNT_FIELDS,
  type BankAccountValues,
} from "./bank-account-schema";
import { emptyBankAccountValues, toFormValues } from "./bank-account-values";
import type { BankAccountDetail, BankAccountListRow } from "./types";

/** React Query key of the list, so a write from the edit page refreshes it. */
export const BANK_ACCOUNT_LIST_QUERY_KEY = ["bankAccount"] as const;

/**
 * The bank accounts of the banking plugin — list and edit — as data (see lib/page-def/types.ts).
 *
 * Replaces the removed React pages (`BankAccountPagesRest`), which it follows: the list's columns, the form's
 * master data, the access of users and groups and the settings of the csv import with their help. The import
 * itself is a page of its own (the React form had a drop area), reached like the transactions of the account
 * from the heading of the form and from the list.
 */
export const BANK_ACCOUNT_PAGE = definePage<
  BankAccountListRow,
  BankAccountValues,
  BankAccountDetail,
  typeof BANK_ACCOUNT_METADATA
>({
  entity: "bankAccount",
  metadata: BANK_ACCOUNT_METADATA,
  route: "/bankAccount",
  queryKey: BANK_ACCOUNT_LIST_QUERY_KEY,
  // Where the entry sits in the main menu: Finance (BankingPlugin, MenuItemDefId.FIBU).
  categoryKey: "menu.fibu._",
  titleKey: "plugins.banking.account.title.list",
  defaultSort: { id: "name", desc: false },
  columns: [
    { name: "name", size: 220, className: "font-semibold" },
    { name: "bank", size: 200 },
    { name: "iban", size: 220 },
    { name: "description", size: 320, wrap: true },
    { name: "lastUpdate", size: 130 },
  ],
  listActions: BankAccountListActions,
  edit: {
    schema: bankAccountSchema,
    fieldNames: BANK_ACCOUNT_FIELDS,
    arrayFieldNames: BANK_ACCOUNT_ARRAY_FIELDS,
    defaultValues: emptyBankAccountValues,
    toFormValues,
    title: (account) => account.name ?? "",
    newTitleKey: "plugins.banking.account.title.add",
    savedMessageKey: "message.successfullChanged",
    clone: true,
    autoFocus: "name",
    crossLinks: [
      {
        labelKey: "plugins.banking.account.record.title.list",
        href: (account) =>
          account.id != null ? bankAccountRecordsHref(account.id) : null,
        prominent: true,
      },
      {
        labelKey: "plugins.banking.account.record.import.title",
        href: (account) =>
          account.id != null ? bankAccountImportHref(account.id) : null,
        prominent: true,
      },
    ],
    sections: [
      {
        id: "general",
        titleKey: "plugins.banking.account._",
        fields: [
          { name: "name", span: 2 },
          { name: "bank" },
          { name: "iban", span: 2 },
          { name: "bic" },
          { name: "description", span: 3, rows: 3 },
        ],
      },
      {
        id: "access",
        titleKey: "access.title._",
        columns: 4,
        fields: [
          { custom: BankAccountFullAccessUsersField, span: 2 },
          { custom: BankAccountFullAccessGroupsField, span: 2 },
          { custom: BankAccountReadonlyAccessUsersField, span: 2 },
          { custom: BankAccountReadonlyAccessGroupsField, span: 2 },
        ],
      },
      {
        id: "importSettings",
        titleKey: "plugins.banking.account.importSettings",
        collapsed: true,
        fields: [
          { name: "importSettings", span: 3, rows: 12 },
          { custom: BankAccountImportSettingsHelp, span: 3 },
        ],
      },
    ],
  },
});
