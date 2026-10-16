import type { AddressValues } from "./address-schema";
import type { AddressDetail } from "./types";

/** The text fields of an address, which the form holds as a string or null. */
export const ADDRESS_TEXT_FIELDS = [
  "name",
  "firstName",
  "birthName",
  "title",
  "email",
  "privateEmail",
  "birthday",
  "communicationLanguage",
  "organization",
  "division",
  "positionText",
  "website",
  "businessPhone",
  "mobilePhone",
  "fax",
  "privatePhone",
  "privateMobilePhone",
  "addressText",
  "addressText2",
  "zipCode",
  "city",
  "country",
  "state",
  "postalAddressText",
  "postalAddressText2",
  "postalZipCode",
  "postalCity",
  "postalCountry",
  "postalState",
  "privateAddressText",
  "privateAddressText2",
  "privateZipCode",
  "privateCity",
  "privateCountry",
  "privateState",
  "fingerprint",
  "publicKey",
  "comment",
] as const satisfies readonly (keyof AddressValues & keyof AddressDetail)[];

export type AddressTextField = (typeof ADDRESS_TEXT_FIELDS)[number];

/**
 * A field Spring left out of the JSON (`JsonInclude.Include.NON_NULL`, see types.ts) arrives as `undefined`;
 * every value is normalised here, so no field ever holds `undefined` — which a controlled input would read as
 * "uncontrolled" and the schema as a missing value. The name is the one mandatory text and stays a string.
 */
export function toFormValues(address: AddressDetail): AddressValues {
  const texts = Object.fromEntries(
    ADDRESS_TEXT_FIELDS.map((name) => [name, address[name] ?? null])
  ) as Record<AddressTextField, string | null>;
  return {
    ...texts,
    name: address.name ?? "",
    id: address.id ?? null,
    uid: address.uid ?? null,
    addressStatus: (address.addressStatus ??
      null) as AddressValues["addressStatus"],
    contactStatus: (address.contactStatus ??
      null) as AddressValues["contactStatus"],
    form: (address.form ?? null) as AddressValues["form"],
    addressbookList: (address.addressbookList ?? []).map((book) => ({
      id: book.id,
      displayName: book.displayName ?? book.title,
    })),
    isFavoriteCard: address.isFavoriteCard ?? false,
    instantMessaging: address.instantMessaging ?? null,
    hasImage: address.hasImage ?? false,
    imageDeleted: false,
    imageDataUrl: null,
    importIndex: importIndexOfUrl(),
    created: address.created ?? null,
  };
}

/**
 * The entry of a running VCF import the form was opened for (`?importIndex=`), see AddressImportEntryIntro.
 *
 * Read from the url here rather than set by a component: the form is reset onto the loaded address after every
 * load, and a value set beside that reset would either be lost or — being a change — keep the reset from happening.
 */
export function importIndexOfUrl(): number | null {
  if (typeof window === "undefined") return null;
  const raw = new URLSearchParams(window.location.search).get("importIndex");
  const index = raw == null ? NaN : Number(raw);
  return Number.isInteger(index) && index >= 0 ? index : null;
}

/**
 * Blank form for an address that doesn't exist yet. An "add" reads its preset from `/rs/address/newEntry`
 * (`AddressEntityRest.newBaseDO`: up to date, active, in the global address book), and the form is reset onto
 * that answer.
 */
export function emptyAddressValues(): AddressValues {
  return toFormValues({ id: null });
}
