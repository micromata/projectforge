import type { AddressDetail, PhoneType } from "./types";

export interface AddressPhoneNumber {
  type: PhoneType;
  number: string;
  /** i18n key of the kind of number. */
  labelKey: string;
  /** A mobile number, which may be sent a text message. */
  mobile: boolean;
}

/**
 * The phone numbers of an address worth showing, in the order of the former view: business, mobile, private,
 * private mobile. The fax is no number to call and is left out, as before. The types are the ones the phone call
 * and the text message pages take (`PhoneType`).
 */
export function phoneNumbersOf(address: AddressDetail): AddressPhoneNumber[] {
  const all: (AddressPhoneNumber | null)[] = [
    entry(
      "BUSINESS",
      address.businessPhone,
      "address.phoneType.business",
      false
    ),
    entry("MOBILE", address.mobilePhone, "address.phoneType.mobile", true),
    entry("PRIVATE", address.privatePhone, "address.phoneType.private", false),
    entry(
      "PRIVATE_MOBILE",
      address.privateMobilePhone,
      "address.phoneType.privateMobile",
      true
    ),
  ];
  return all.filter((value): value is AddressPhoneNumber => value !== null);
}

function entry(
  type: PhoneType,
  number: string | null | undefined,
  labelKey: string,
  mobile: boolean
): AddressPhoneNumber | null {
  const trimmed = number?.trim();
  return trimmed ? { type, number: trimmed, labelKey, mobile } : null;
}
