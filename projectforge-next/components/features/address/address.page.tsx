import { definePage } from "@/lib/page-def/define-page";
import { ADDRESS_IMPORT_QUERY_KEY } from "@/lib/rs/address-import";
import {
  AddressAddressbooksField,
  AddressFavoriteField,
} from "./address-addressbooks-field";
import { AddressBlocksField } from "./address-blocks-field";
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
import { AddressOrganizationField } from "./address-suggest-fields";
import { emptyAddressValues, toFormValues } from "./address-values";
import type { AddressDetail, AddressListRow } from "./types";

/**
 * The whole address page — list and form — as data (see lib/page-def/types.ts). A row opens the view of the
 * address in a dialog (AddressViewDialog; `/address/{id}` is the same view as a page), whose "edit" leads to the
 * form (`/address/{id}/edit`).
 *
 * The list follows the former server side layout (`AddressPagesRest`): the filters (my favorites, doublets, with
 * images, my entries) come from the backend, a favorite of the logged-in user is tinted. The form has the fields
 * of the former form in the same cards, except the three addresses, which share one (AddressBlocksField); custom
 * are the address books with the favorite flag, the language, the image and the fields completed from other
 * addresses.
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
  // import page, which the form returns to, must read its state anew. The list of the address campaign values
  // (ADDRESS_CAMPAIGN_VALUE_LIST_QUERY_KEY, spelled out to keep the plugin page out of this bundle) shows the
  // address and whether it is valid, and is returned to as well.
  extraInvalidateKeys: [ADDRESS_IMPORT_QUERY_KEY, ["addressCampaignValue"]],
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
    // The list by default; the address campaign values open the address in the same dialog over their list and
    // return there (`?returnTo=/address-campaign-value`, AddressCampaignValueAddressCell), as the React list's
    // modal kept the user on the campaign.
    returnTargets: [
      { route: ADDRESS_ROUTE, labelKey: "address.title.list" },
      {
        route: "/address-campaign-value",
        labelKey: "plugins.marketing.addressCampaignValue.title.list",
      },
    ],
    sections: [
      {
        id: "status",
        titleKey: "status",
        fields: [
          { name: "addressStatus" },
          { name: "contactStatus" },
          { custom: AddressFavoriteField },
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
        // The business, postal and private address in one card: only the kinds holding a value are shown,
        // further ones are added there (AddressBlocksField).
        id: "addresses",
        titleKey: "address.addresses",
        fields: [{ custom: AddressBlocksField, span: 3 }],
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
