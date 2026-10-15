import { LICENSE_METADATA } from "@/lib/metadata/license.generated";
import { definePage } from "@/lib/page-def/define-page";
import { LicenseFilesField } from "./license-files-field";
import { LicenseKeyField } from "./license-key-field";
import { LicenseOwnersField } from "./license-owners-field";
import {
  licenseSchema,
  LICENSE_FIELDS,
  type LicenseValues,
} from "./license-schema";
import {
  LicenseOrganizationField,
  LicenseProductField,
} from "./license-suggest-fields";
import { emptyLicenseValues, toFormValues } from "./license-values";
import type { LicenseDetail, LicenseListRow } from "./types";

/** React Query key of the list, so a write from the edit page refreshes it. */
export const LICENSE_LIST_QUERY_KEY = ["license"] as const;

/**
 * The whole license page of the licensemanagement plugin ("Licenses / Hardware") — list and edit — as
 * data (see lib/page-def/types.ts).
 *
 * Replaces the removed Wicket pages, which it follows: the list shows `LicenseListPage`'s columns in its
 * order, sorted by organization as it did (`orderString`: organization, product, version), the key column
 * for administrators only; the form has `LicenseEditForm`'s fields. Labels and every rule come from
 * LicenseDO through the generated metadata.
 */
export const LICENSE_PAGE = definePage<
  LicenseListRow,
  LicenseValues,
  LicenseDetail,
  typeof LICENSE_METADATA
>({
  entity: "license",
  metadata: LICENSE_METADATA,
  route: "/license",
  queryKey: LICENSE_LIST_QUERY_KEY,
  // Where the entry sits in the main menu: Misc (LicenseManagementPlugin, MenuItemDefId.MISC).
  categoryKey: "menu.misc",
  titleKey: "plugins.licensemanagement.title.list",
  defaultSort: { id: "organization" },
  columns: [
    { name: "organization", size: 200 },
    { name: "product", size: 220, className: "font-semibold" },
    { name: "version", size: 120 },
    { name: "numberOfLicenses", size: 90 },
    {
      // The owners by name, as the legacy list showed them. No sorting: the backend orders by entity
      // property, and the DO only has the csv of their ids.
      id: "owners",
      labelKey: "plugins.licensemanagement.owner",
      accessor: (row) => row.owners?.map((user) => user.displayName).join(", "),
      size: 240,
      sortable: false,
    },
    { name: "device", size: 200 },
    {
      // Administrators only, as on the legacy list — what the backend answers
      // (`LicenseEntityRest.addVariablesForListPage`). An owner reads the key on the form.
      name: "key",
      size: 240,
      visible: (ctx) => ctx.variables?.keyColumnVisible === true,
    },
    {
      // How many of the two files are stored, empty for none — the files themselves are on the form.
      // No sorting: the count is computed, no entity property.
      id: "numberOfFiles",
      labelKey: "plugins.licensemanagement.files",
      accessor: (row) => row.numberOfFiles ?? undefined,
      size: 80,
      align: "right",
      sortable: false,
    },
    { name: "comment", size: 320 },
    { name: "created", size: 130 },
    { name: "lastUpdate", size: 130 },
  ],
  edit: {
    schema: licenseSchema,
    fieldNames: LICENSE_FIELDS,
    arrayFieldNames: ["owners"],
    defaultValues: emptyLicenseValues,
    toFormValues,
    title: (license) =>
      [license.organization, license.product, license.version]
        .filter(Boolean)
        .join(" "),
    newTitleKey: "plugins.licensemanagement.title.add",
    savedMessageKey: "message.successfullChanged",
    clone: true,
    sections: [
      {
        id: "general",
        titleKey: "plugins.licensemanagement.license",
        fields: [
          { custom: LicenseOrganizationField },
          { custom: LicenseProductField },
          { name: "version" },
          {
            name: "updateFromVersion",
            hintKey: "plugins.licensemanagement.updateFromVersion.tooltip",
          },
          {
            name: "device",
            hintKey: "plugins.licensemanagement.device.tooltip",
          },
          { name: "numberOfLicenses", maxDigits: 6 },
          { custom: LicenseOwnersField, span: 3 },
          { name: "validSince" },
          { name: "validUntil" },
          { name: "licenseHolder" },
          { custom: LicenseKeyField, span: 3 },
          { name: "comment", span: 3, rows: 4 },
        ],
      },
      {
        // Only for who may see the key, as Wicket showed the upload panels — the backend refuses the
        // file endpoints to anybody else anyway.
        id: "files",
        titleKey: "plugins.licensemanagement.files",
        visible: ({ data }) => data?.keyVisible !== false,
        fields: [{ custom: LicenseFilesField, span: 3 }],
      },
    ],
  },
});
