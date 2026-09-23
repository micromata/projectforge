import { KUNDE_METADATA } from "@/lib/metadata/kunde.generated";
import { definePage } from "@/lib/page-def/define-page";
import { CustomerKontoField } from "./customer-konto-field";
import { CustomerNumberField } from "./customer-number-field";
import {
  customerSchema,
  CUSTOMER_FIELDS,
  type CustomerValues,
} from "./customer-schema";
import { emptyCustomerValues, toFormValues } from "./customer-values";
import type { CustomerDetail, CustomerListRow } from "./types";

/** React Query key of the list, so a write from the edit page refreshes it. */
export const CUSTOMER_LIST_QUERY_KEY = ["customer"] as const;

/**
 * The whole customer page — list and edit — as data (see lib/page-def/types.ts).
 *
 * The columns are those of `CustomerPagesRest.createListLayout` in its order, the form is its
 * `createEditLayout`; every label, the status texts and every rule come from KundeDO through the
 * generated metadata. Declared here is order and width, plus the two fields the declaration cannot
 * describe: the number (editable only while adding — see CustomerNumberField) and the account (a
 * foreign DO with no metadata — see CustomerKontoField).
 *
 * One section and no attachments: the entity has no `jcrPath`. Its change history is a tab of its
 * own, which `KUNDE_METADATA.historizable` says.
 *
 * Customer favorites (`UserPrefArea.KUNDE_FAVORITE`) are deliberately not carried over — the same
 * decision as the task tree's favorites; the list still offers the generic saved-filter favorites.
 */
export const CUSTOMER_PAGE = definePage<
  CustomerListRow,
  CustomerValues,
  CustomerDetail,
  typeof KUNDE_METADATA
>({
  entity: "customer",
  metadata: KUNDE_METADATA,
  route: "/customer",
  queryKey: CUSTOMER_LIST_QUERY_KEY,
  // Where the entry sits in the main menu: Finance > Customers (MenuCreator, CUSTOMER_LIST).
  categoryKey: "menu.fibu",
  titleKey: "fibu.kunde.title.list",
  columns: [
    // The formatted number ("5.###"), read as one — the same value the legacy list leads with. The
    // raw `nummer` behind it is the sort property, so the column keeps the metadata order.
    { name: "kost", size: 110, className: "font-mono" },
    { name: "identifier", size: 140 },
    { name: "name", size: 260 },
    { name: "division", size: 180 },
    {
      // The account, shown by its display name as the legacy list formats it (KONTO formatter). No
      // sorting: the backend orders by entity property, and a referenced DO is none.
      id: "konto",
      labelKey: "fibu.konto",
      accessor: (row) => row.konto?.displayName,
      size: 200,
      sortable: false,
    },
    // The enum cell renders the translated status label (KundeStatus), so no separate statusAsString.
    { name: "status", size: 120 },
    { name: "description", size: 360, wrap: true },
    // When the customer was created and last changed — the two the legacy list omits.
    { name: "created", size: 130 },
    { name: "lastUpdate", size: 130 },
  ],
  edit: {
    schema: customerSchema,
    fieldNames: CUSTOMER_FIELDS,
    defaultValues: emptyCustomerValues,
    toFormValues,
    // The name is what identifies a customer to a reader — the same string the list shows.
    title: (customer) => customer.name ?? "",
    newTitleKey: "fibu.kunde.title.add",
    savedMessageKey: "message.successfullChanged",
    sections: [
      {
        id: "general",
        titleKey: "fibu.kunde._",
        fields: [
          { custom: CustomerNumberField },
          { name: "name", span: 2 },
          { custom: CustomerKontoField, span: 2 },
          { name: "identifier" },
          { name: "division", span: 2 },
          // The one value a reader looks for first — where the customer stands in its lifecycle.
          { name: "status", emphasized: true },
          { name: "description", span: 3, rows: 4 },
        ],
      },
    ],
  },
});
