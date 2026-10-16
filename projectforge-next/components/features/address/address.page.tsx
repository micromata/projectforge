import { definePage } from "@/lib/page-def/define-page";
import { ADDRESS_IMPORT_QUERY_KEY } from "@/lib/rs/address-import";
import { AddressAddressbooksField } from "./address-addressbooks-field";
import { ADDRESS_COLUMNS } from "./address-columns";
import { AddressEditIntro } from "./address-edit-intro";
import { AddressGearMenuActions } from "./address-gear-menu-actions";
import { AddressImageField } from "./address-image-field";
import { AddressLanguageField } from "./address-language-field";
import { AddressListActions } from "./address-list-actions";
import { AddressViewDialog } from "./address-view-dialog";
import { ADDRESS_LIST_QUERY_KEY, ADDRESS_ROUTE } from "./address-routes";
import {
  ADDRESS_FORM_METADATA,
  addressSchema,
  ADDRESS_FIELDS,
  type AddressValues,
} from "./address-schema";
import {
  AddressOrganizationField,
  AddressText2Field,
  AddressTextField,
  PostalAddressText2Field,
  PostalAddressTextField,
  PrivateAddressText2Field,
  PrivateAddressTextField,
} from "./address-suggest-fields";
import { emptyAddressValues, toFormValues } from "./address-values";
import type { AddressDetail, AddressListRow } from "./types";

/**
 * The whole address page — list and form — as data (see lib/page-def/types.ts). A row opens the view of the
 * address in a dialog (AddressViewDialog; `/address/{id}` is the same view as a page), whose "edit" leads to the
 * form (`/address/{id}/edit`).
 *
 * The list follows the former server side layout (`AddressPagesRest`): the filters (my favorites, doublets, with
 * images, my entries) come from the backend, a favorite of the logged-in user is tinted. The form has the fields
 * of the former form in the same cards; custom are the address books with the favorite flag, the language, the
 * image and the fields completed from other addresses.
 */
export const ADDRESS_PAGE = definePage<
  AddressListRow,
  AddressValues,
  AddressDetail,
  typeof ADDRESS_FORM_METADATA
>({
  entity: "address",
  metadata: ADDRESS_FORM_METADATA,
  route: ADDRESS_ROUTE,
  queryKey: ADDRESS_LIST_QUERY_KEY,
  // Common > Addresses (MenuCreator, ADDRESS_LIST).
  categoryKey: "menu.common",
  titleKey: "address.title.list",
  defaultSort: { id: "name", desc: false },
  columns: ADDRESS_COLUMNS,
  rowClassName: (row) => (row.isFavoriteCard ? "row-blue" : undefined),
  // A row opens the view in a dialog over the list, as the React list did; `/address/{id}` stays the page.
  rowDialog: AddressViewDialog,
  legend: [{ className: "row-blue", labelKey: "address.filter.myFavorites" }],
  listActions: AddressListActions,
  gearMenuActions: AddressGearMenuActions,
  massUpdate: { endpoint: "addressSelected", route: "/address/mass-update" },
  // A save from the form of an import entry marks it as imported (AddressSaveSupport.reconcileImport): the
  // import page, which the form returns to, must read its state anew.
  extraInvalidateKeys: [ADDRESS_IMPORT_QUERY_KEY],
  edit: {
    schema: addressSchema,
    fieldNames: ADDRESS_FIELDS,
    arrayFieldNames: ["addressbookList"],
    defaultValues: emptyAddressValues,
    toFormValues,
    title: (address) =>
      [address.firstName, address.name].filter(Boolean).join(" ") ||
      address.organization ||
      "",
    newTitleKey: "address.title.add",
    savedMessageKey: "message.successfullChanged",
    autoFocus: "name",
    clone: true,
    // AddressDao is one of the two DAOs that support it (`isForceDeletionSupport`).
    forceDelete: true,
    editIntro: AddressEditIntro,
    sections: [
      {
        id: "status",
        titleKey: "status",
        fields: [
          { name: "addressStatus" },
          { name: "contactStatus" },
          { custom: AddressAddressbooksField, span: 3 },
        ],
      },
      {
        id: "personalData",
        titleKey: "address.heading.personalData",
        fields: [
          { name: "name" },
          { name: "firstName" },
          { name: "birthName" },
          { name: "form" },
          { name: "title" },
          { name: "birthday" },
          { name: "email" },
          // The entity labels both just "e-mail".
          { name: "privateEmail", labelKey: "address.privateEmail" },
          { custom: AddressLanguageField },
        ],
      },
      {
        id: "businessContact",
        titleKey: "address.heading.businessContact",
        fields: [
          { custom: AddressOrganizationField },
          { name: "division" },
          { name: "positionText" },
          { name: "website" },
        ],
      },
      {
        id: "phoneNumbers",
        titleKey: "address.phoneNumbers",
        fields: [
          // The entity labels the business and the private number alike ("phone"); the kinds spelled out.
          { name: "businessPhone", labelKey: "address.phoneType.business" },
          { name: "mobilePhone", labelKey: "address.phoneType.mobile" },
          { name: "fax", labelKey: "address.phoneType.fax" },
          { name: "privatePhone", labelKey: "address.phoneType.private" },
          {
            name: "privateMobilePhone",
            labelKey: "address.phoneType.privateMobile",
          },
        ],
      },
      {
        id: "businessAddress",
        titleKey: "address.heading.businessAddress",
        fields: [
          { custom: AddressTextField },
          { custom: AddressText2Field },
          { name: "zipCode", startsRow: true },
          { name: "city" },
          { name: "country", startsRow: true },
          { name: "state" },
        ],
      },
      {
        id: "postalAddress",
        titleKey: "address.heading.postalAddress",
        fields: [
          { custom: PostalAddressTextField },
          { custom: PostalAddressText2Field },
          { name: "postalZipCode", startsRow: true },
          { name: "postalCity" },
          { name: "postalCountry", startsRow: true },
          { name: "postalState" },
        ],
      },
      {
        id: "privateAddress",
        titleKey: "address.heading.privateAddress",
        fields: [
          { custom: PrivateAddressTextField },
          { custom: PrivateAddressText2Field },
          { name: "privateZipCode", startsRow: true },
          { name: "privateCity" },
          { name: "privateCountry", startsRow: true },
          { name: "privateState" },
        ],
      },
      {
        id: "image",
        titleKey: "address.image",
        fields: [{ custom: AddressImageField, span: 3 }],
      },
      {
        id: "publicKey",
        titleKey: "address.publicKey",
        fields: [
          { name: "fingerprint", span: 3 },
          { name: "publicKey", span: 3, rows: 6 },
        ],
      },
      {
        id: "comment",
        titleKey: "comment",
        fields: [{ name: "comment", span: 3, rows: 5 }],
      },
    ],
  },
});
