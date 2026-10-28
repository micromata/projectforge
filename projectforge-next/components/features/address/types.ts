/** An address book as the address DTO carries it: the id is what `copyTo` stores. */
// A type alias, not an interface: only an alias is assignable to the schema's loose object.
export type AddressbookRef = {
  id: number;
  displayName?: string;
  title?: string;
};

/**
 * org.projectforge.rest.dto.Address. Spring leaves empty fields out of the JSON (`JsonInclude.NON_NULL`), so
 * every property is optional.
 */
export interface AddressDetail {
  id: number | null;
  uid?: string | null;
  contactStatus?: string | null;
  addressStatus?: string | null;
  name?: string | null;
  /** The name including the former one (maiden name), read only. */
  fullLastName?: string | null;
  firstName?: string | null;
  birthName?: string | null;
  form?: string | null;
  title?: string | null;
  positionText?: string | null;
  organization?: string | null;
  division?: string | null;
  businessPhone?: string | null;
  mobilePhone?: string | null;
  fax?: string | null;
  addressText?: string | null;
  addressText2?: string | null;
  zipCode?: string | null;
  city?: string | null;
  country?: string | null;
  state?: string | null;
  postalAddressText?: string | null;
  postalAddressText2?: string | null;
  postalZipCode?: string | null;
  postalCity?: string | null;
  postalCountry?: string | null;
  postalState?: string | null;
  email?: string | null;
  website?: string | null;
  /** A locale as `Locale.toString()` writes it, e.g. `de_DE`. */
  communicationLanguage?: string | null;
  privatePhone?: string | null;
  privateMobilePhone?: string | null;
  privateAddressText?: string | null;
  privateAddressText2?: string | null;
  privateZipCode?: string | null;
  privateCity?: string | null;
  privateCountry?: string | null;
  privateState?: string | null;
  privateEmail?: string | null;
  publicKey?: string | null;
  fingerprint?: string | null;
  comment?: string | null;
  /** ISO date `yyyy-MM-dd`. */
  birthday?: string | null;
  instantMessaging?: unknown;
  addressbookList?: AddressbookRef[] | null;
  /** Whether the address is a favorite of the logged-in user. */
  isFavoriteCard?: boolean;
  /** Whether an image is stored for the address. */
  hasImage?: boolean;
  /** Translated, read only. */
  addressStatusAsString?: string | null;
  contactStatusAsString?: string | null;
  writeAccess?: boolean | null;
  deleteAccess?: boolean | null;
  created?: string | null;
  lastUpdate?: string | null;
  deleted?: boolean;
}

/** A row of the list: the same DTO, with the id the table keys rows by. */
export interface AddressListRow extends AddressDetail {
  id: number;
}

/** The phone numbers of an address, in the order the view and the list show them. */
export type PhoneType = "BUSINESS" | "MOBILE" | "PRIVATE" | "PRIVATE_MOBILE";
