import { KONTO_METADATA } from "@/lib/metadata/konto.generated";
import { definePage } from "@/lib/page-def/define-page";
import { AccountSellerBankAccountField } from "./account-seller-bank-account-field";
import {
  accountSchema,
  ACCOUNT_FIELDS,
  type AccountValues,
} from "./account-schema";
import { emptyAccountValues, toFormValues } from "./account-values";
import type { KontoDetail, KontoListRow } from "./types";

/** React Query key of the list, so a write from the edit page refreshes it. */
export const ACCOUNT_LIST_QUERY_KEY = ["account"] as const;

/**
 * The whole account page — list and edit — as data (see lib/page-def/types.ts).
 *
 * Replaces the server-laid-out React page (`KontoPagesRest`) and follows the legacy Wicket pages,
 * which stay reachable as the classic version: the list shows `KontoListPage`'s columns in its order,
 * sorted by number as it does; the form has `KontoEditForm`'s two blocks, including what the React page
 * lacked — the seller's bank account as a select over the configured accounts and the Leitweg-ID's help
 * text. Labels, the status texts and every rule come from KontoDO through the generated metadata.
 */
export const ACCOUNT_PAGE = definePage<
  KontoListRow,
  AccountValues,
  KontoDetail,
  typeof KONTO_METADATA
>({
  entity: "account",
  metadata: KONTO_METADATA,
  route: "/account",
  queryKey: ACCOUNT_LIST_QUERY_KEY,
  // Where the entry sits in the main menu: Finance > Cost (MenuCreator, MenuItemDefId.COST).
  categoryKey: "menu.fibu.kost",
  titleKey: "fibu.konto.title.list",
  defaultSort: { id: "nummer" },
  columns: [
    { name: "nummer", size: 120, className: "font-mono font-semibold" },
    { name: "status", size: 110 },
    { name: "bezeichnung", size: 260 },
    { name: "description", size: 400 },
    { name: "created", size: 130 },
    { name: "lastUpdate", size: 130 },
  ],
  edit: {
    schema: accountSchema,
    fieldNames: ACCOUNT_FIELDS,
    defaultValues: emptyAccountValues,
    toFormValues,
    // The number identifies the account; its name is the readable half of the same heading.
    title: (konto) =>
      [konto.nummer, konto.bezeichnung]
        .filter((v) => v != null && v !== "")
        .join(" "),
    newTitleKey: "fibu.konto.title.add",
    savedMessageKey: "message.successfullChanged",
    sections: [
      {
        id: "general",
        titleKey: "fibu.konto",
        fields: [
          { name: "nummer" },
          { name: "status" },
          { name: "bezeichnung", span: 3 },
          { name: "description", span: 3, rows: 4 },
        ],
      },
      {
        // What an e-invoice to this account is addressed with (Wicket's heading `fibu.konto.eInvoice`).
        id: "eInvoice",
        titleKey: "fibu.konto.eInvoice",
        fields: [
          { name: "contactPerson", span: 2 },
          // Beside the contact person; the cell after the e-invoice address stays empty.
          { custom: AccountSellerBankAccountField },
          { name: "street", span: 3 },
          { name: "zipCode" },
          { name: "city", span: 2 },
          { name: "country" },
          { name: "vatId" },
          { name: "leitwegId", hintKey: "fibu.konto.leitwegId.tooltip" },
          { name: "eInvoiceEmail", span: 2 },
        ],
      },
    ],
  },
});
