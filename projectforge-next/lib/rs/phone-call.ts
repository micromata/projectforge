/**
 * The "Direct call" page (`org.projectforge.rest.PhoneCallRest`), successor of Wicket's `wa/phoneCall`.
 * A non-entity, standalone page, so it has its own small client here rather than going through
 * `fetchList` / the entity plumbing (as `send-text-message.ts` does for the SMS page).
 */

import { request } from "./client";
import type {
  AcItem,
  AddressInfo,
  CallRequest,
  CallResult,
  PhoneCallInitialData,
  PhoneCallParams,
} from "@/components/features/phone-call/types";

function toParams(params: PhoneCallParams): string {
  const query = new URLSearchParams();
  if (params.addressId != null)
    query.set("addressId", String(params.addressId));
  if (params.number) query.set("number", params.number);
  if (params.callerPage) query.set("callerPage", params.callerPage);
  const s = query.toString();
  return s ? `?${s}` : "";
}

/** Initial form data: the prefilled number, the resolved address, and the user's phone / caller ids. */
export function fetchPhoneCallData(
  params: PhoneCallParams,
  signal?: AbortSignal
): Promise<PhoneCallInitialData> {
  return request<PhoneCallInitialData>(
    `/rs/phoneCall${toParams(params)}`,
    { method: "GET" },
    signal
  );
}

/**
 * Number suggestions: one entry per matching address number (`display` shown, `number` dialed, `addressId` for
 * the panel); an empty search offers the numbers recently called, which carry no address.
 */
export function suggestNumbers(
  search: string,
  signal?: AbortSignal
): Promise<AcItem[]> {
  return request<AcItem[]>(
    `/rs/phoneCall/ac?search=${encodeURIComponent(search)}`,
    { method: "GET" },
    signal
  );
}

/**
 * The address behind a picked suggestion, so the address panel follows the selection. Passing the picked
 * `number` lets the backend remember the pair as the last shown address (restored on re-open) and returns
 * `null` if the number no longer belongs to the address.
 */
export function fetchAddress(
  id: number,
  number?: string,
  signal?: AbortSignal
): Promise<AddressInfo | null> {
  const query = new URLSearchParams({ id: String(id) });
  if (number) query.set("number", number);
  return request<AddressInfo | null>(
    `/rs/phoneCall/address?${query.toString()}`,
    { method: "GET" },
    signal
  );
}

/** Places the call; the result carries a localized success or error text for the toast. */
export function placeCall(
  body: CallRequest,
  signal?: AbortSignal
): Promise<CallResult> {
  return request<CallResult>(
    "/rs/phoneCall/call",
    { method: "POST", body: JSON.stringify(body) },
    signal
  );
}
