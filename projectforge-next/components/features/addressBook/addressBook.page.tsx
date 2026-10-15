import { ADDRESSBOOK_METADATA } from "@/lib/metadata/addressbook.generated";
import { definePage } from "@/lib/page-def/define-page";
import {
  AddressbookFullAccessGroupsField,
  AddressbookFullAccessUsersField,
  AddressbookReadonlyAccessGroupsField,
  AddressbookReadonlyAccessUsersField,
} from "./addressbook-access-fields";
import {
  addressbookSchema,
  ADDRESSBOOK_FIELDS,
  type AddressbookValues,
} from "./addressbook-schema";
import { emptyAddressbookValues, toFormValues } from "./addressbook-values";
import type {
  AddressbookDetail,
  AddressbookListRow,
  AddressbookRef,
} from "./types";

/** React Query key of the list, so a write from the edit page refreshes it. */
export const ADDRESS_BOOK_LIST_QUERY_KEY = ["addressBook"] as const;

/** The display names of an access list, as one cell. */
function names(refs: AddressbookRef[] | undefined): string {
  return (refs ?? []).map((ref) => ref.displayName ?? "???").join(", ");
}

/**
 * The address books — list and edit — as data (see lib/page-def/types.ts).
 *
 * Replaces the React UILayout pages of `AddressBookPagesRest`, which it follows: the list's columns (the last
 * update moved to the end, behind the access lists), and the form's title, owner and description and the access
 * rights of users and groups to the addresses of the address book.
 */
export const ADDRESS_BOOK_PAGE = definePage<
  AddressbookListRow,
  AddressbookValues,
  AddressbookDetail,
  typeof ADDRESSBOOK_METADATA
>({
  entity: "addressBook",
  metadata: ADDRESSBOOK_METADATA,
  route: "/addressBook",
  queryKey: ADDRESS_BOOK_LIST_QUERY_KEY,
  // Where the entry sits in the main menu: Common (MenuCreator, MenuItemDefId.ADDRESSBOOK_LIST).
  categoryKey: "menu.common",
  titleKey: "addressbook.title.list",
  defaultSort: { id: "title", desc: false },
  columns: [
    { name: "title", size: 220, className: "font-semibold" },
    { name: "description", size: 300, wrap: true },
    {
      name: "owner",
      size: 170,
      cell: ({ row }) => row.original.owner?.displayName ?? null,
    },
    // The access lists are csv lists of ids in the database, resolved by the DTO: nothing to sort by.
    {
      id: "fullAccessUsers",
      labelKey: "addressbook.fullAccess.users",
      accessor: (row) => names(row.fullAccessUsers),
      sortable: false,
      size: 200,
      wrap: true,
    },
    {
      id: "fullAccessGroups",
      labelKey: "addressbook.fullAccess.groups",
      accessor: (row) => names(row.fullAccessGroups),
      sortable: false,
      size: 200,
      wrap: true,
    },
    {
      id: "readonlyAccessUsers",
      labelKey: "addressbook.readonlyAccess.users",
      accessor: (row) => names(row.readonlyAccessUsers),
      sortable: false,
      size: 200,
      wrap: true,
    },
    {
      id: "readonlyAccessGroups",
      labelKey: "addressbook.readonlyAccess.groups",
      accessor: (row) => names(row.readonlyAccessGroups),
      sortable: false,
      size: 200,
      wrap: true,
    },
    { name: "lastUpdate", size: 130 },
  ],
  edit: {
    schema: addressbookSchema,
    fieldNames: ADDRESSBOOK_FIELDS,
    arrayFieldNames: [
      "fullAccessUsers",
      "fullAccessGroups",
      "readonlyAccessUsers",
      "readonlyAccessGroups",
    ],
    defaultValues: emptyAddressbookValues,
    toFormValues,
    title: (ab) => ab.title ?? "",
    newTitleKey: "addressbook.title.add",
    savedMessageKey: "message.successfullChanged",
    sections: [
      {
        id: "general",
        titleKey: "addressbook.title.heading",
        fields: [
          { name: "title", span: 2 },
          { name: "owner" },
          { name: "description", span: 3, rows: 3 },
        ],
      },
      {
        id: "access",
        titleKey: "access.title._",
        columns: 4,
        fields: [
          { custom: AddressbookFullAccessUsersField, span: 2 },
          { custom: AddressbookFullAccessGroupsField, span: 2 },
          { custom: AddressbookReadonlyAccessUsersField, span: 2 },
          { custom: AddressbookReadonlyAccessGroupsField, span: 2 },
        ],
      },
    ],
  },
});
