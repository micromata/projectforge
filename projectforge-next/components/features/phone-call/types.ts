/** Contract of org.projectforge.rest.PhoneCallRest. */

/** Deep-link parameters that seed the call (see the phone-number link on an address). */
export interface PhoneCallParams {
  addressId?: number;
  number?: string;
  callerPage?: string;
}

/** One phone number of the resolved address: PhoneCallRest.AddressPhoneNumber. */
export interface AddressPhoneNumber {
  number: string;
  phoneType: "BUSINESS" | "MOBILE" | "PRIVATE" | "PRIVATE_MOBILE";
}

/** The resolved address shown in the address panel: PhoneCallRest.AddressInfo. */
export interface AddressInfo {
  id: number;
  fullName: string;
  numbers: AddressPhoneNumber[];
}

/** Initial form data: PhoneCallRest.InitialData. */
export interface PhoneCallInitialData {
  phoneNumber?: string | null;
  address?: AddressInfo | null;
  myPhoneIds: string[];
  callerIds: string[];
  recentMyPhoneId?: string | null;
  recentMyCallerId?: string | null;
  sipgateConfigured: boolean;
  callerPage?: string | null;
}

/**
 * One auto-completion entry: PhoneCallRest.AcItem. `display` is what the box shows and holds, `number` the
 * clean number to dial, `addressId` (when the entry is an address, not a recent) lets the panel follow the pick.
 */
export interface AcItem {
  addressId?: number | null;
  number: string;
  display: string;
}

/** Body of the call: PhoneCallRest.CallRequest. */
export interface CallRequest {
  phoneNumber: string;
  myPhoneId?: string;
  myCallerId?: string;
}

/** Result of the call: PhoneCallRest.CallResult (`message` is already localized). */
export interface CallResult {
  success: boolean;
  message: string;
}
