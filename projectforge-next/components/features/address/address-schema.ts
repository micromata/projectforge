import { z } from "zod";
import { ADDRESS_METADATA } from "@/lib/metadata/address.generated";
import { fromMetadata } from "@/lib/validation/from-metadata";

/**
 * Mandatory and maximum length come from AddressDO through `lib/metadata/address.generated.ts`. Which fields the
 * form has mirrors org.projectforge.rest.dto.Address.
 *
 * Every property the DTO writes back is part of it, also where nothing renders it (`uid`, the instant
 * messaging): a save posts these values *as* the DTO, so a field left out here would reach `Address.copyTo` as
 * null and overwrite the stored one — the uid is what CardDAV clients know an address by.
 */
const m = fromMetadata(ADDRESS_METADATA);

const addressbookRef = z.looseObject({
  id: z.number(),
  displayName: z.string().optional(),
});

export const addressSchema = z.object({
  // null while the address is new — Spring assigns the id on the first save.
  id: z.number().nullable(),
  uid: z.string().nullable(),
  addressStatus: m.enumField("addressStatus"),
  contactStatus: m.enumField("contactStatus"),
  addressbookList: z.array(addressbookRef),
  isFavoriteCard: z.boolean(),
  name: m.requiredString("name"),
  firstName: m.nullableString("firstName"),
  birthName: m.nullableString("birthName"),
  form: m.enumField("form"),
  title: m.nullableString("title"),
  email: m.nullableString("email"),
  privateEmail: m.nullableString("privateEmail"),
  birthday: z.string().nullable(),
  communicationLanguage: z.string().nullable(),
  organization: m.nullableString("organization"),
  division: m.nullableString("division"),
  positionText: m.nullableString("positionText"),
  website: m.nullableString("website"),
  businessPhone: m.nullableString("businessPhone"),
  mobilePhone: m.nullableString("mobilePhone"),
  fax: m.nullableString("fax"),
  privatePhone: m.nullableString("privatePhone"),
  privateMobilePhone: m.nullableString("privateMobilePhone"),
  addressText: m.nullableString("addressText"),
  addressText2: m.nullableString("addressText2"),
  zipCode: m.nullableString("zipCode"),
  city: m.nullableString("city"),
  country: m.nullableString("country"),
  state: m.nullableString("state"),
  postalAddressText: m.nullableString("postalAddressText"),
  postalAddressText2: m.nullableString("postalAddressText2"),
  postalZipCode: m.nullableString("postalZipCode"),
  postalCity: m.nullableString("postalCity"),
  postalCountry: m.nullableString("postalCountry"),
  postalState: m.nullableString("postalState"),
  privateAddressText: m.nullableString("privateAddressText"),
  privateAddressText2: m.nullableString("privateAddressText2"),
  privateZipCode: m.nullableString("privateZipCode"),
  privateCity: m.nullableString("privateCity"),
  privateCountry: m.nullableString("privateCountry"),
  privateState: m.nullableString("privateState"),
  fingerprint: m.nullableString("fingerprint"),
  publicKey: m.nullableString("publicKey"),
  comment: m.nullableString("comment"),
  instantMessaging: z.unknown(),
  /** Whether an image is stored; the image field shows it. */
  hasImage: z.boolean(),
  /** The stored image is removed by the save (see AddressImageField). */
  imageDeleted: z.boolean(),
  /**
   * The preview url of an image uploaded to the session and not saved yet (the DTO's `imageDataUrl`), null if
   * there is none. Set by the image field and by the text parser taking the image of a vCard over.
   */
  imageDataUrl: z.string().nullable(),
  /** The entry of a running VCF import the form was opened for, which the save marks as imported. */
  importIndex: z.number().nullable(),
  created: z.string().nullable(),
});

export type AddressValues = z.infer<typeof addressSchema>;

/**
 * Field names of the form, so a server validation error can be checked against what actually renders
 * (see applyServerValidationErrors) instead of vanishing into a field nobody sees.
 */
export const ADDRESS_FIELDS = Object.keys(
  addressSchema.shape
) as readonly (keyof AddressValues)[];
