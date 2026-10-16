import type { ImportColumn } from "@/components/shared/import/import-types";

/**
 * The columns of the import list, as the former import page had them: the names, and the addresses, e-mails and
 * phone numbers each in one column (`AddressImportDTO.formatted*`), each with the old → new diff of a modified
 * entry. What changed besides them is listed in a column of its own (`additionalChanges`).
 */
export const ADDRESS_IMPORT_COLUMNS: ImportColumn[] = [
  { field: "name", headerKey: "name", kind: "text", diff: true, width: 150 },
  {
    field: "firstName",
    headerKey: "firstName",
    kind: "text",
    diff: true,
    width: 140,
  },
  {
    field: "organization",
    headerKey: "organization",
    kind: "text",
    diff: true,
    width: 180,
  },
  {
    field: "formattedBusinessAddress",
    headerKey: "address.heading.businessAddress",
    kind: "text",
    diff: true,
    width: 240,
  },
  {
    field: "formattedEmails",
    headerKey: "address.emails",
    kind: "text",
    diff: true,
    width: 220,
  },
  {
    field: "formattedPhones",
    headerKey: "address.phoneNumbers",
    kind: "text",
    diff: true,
    width: 200,
  },
  {
    field: "formattedPrivateAddress",
    headerKey: "address.heading.privateAddress",
    kind: "text",
    diff: true,
    width: 240,
  },
  {
    field: "formattedPostalAddress",
    headerKey: "address.heading.postalAddress",
    kind: "text",
    diff: true,
    width: 240,
  },
  {
    field: "comment",
    headerKey: "comment",
    kind: "text",
    diff: true,
    width: 200,
  },
];
