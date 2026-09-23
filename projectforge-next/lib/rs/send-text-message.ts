/**
 * The "Send text message" page (`org.projectforge.rest.SendTextMessageRest`), successor of Wicket's
 * `wa/sendSms`. A non-entity, standalone page, so it has its own small client here rather than going
 * through `fetchList` / the entity plumbing.
 */

import { request } from "./client";
import type {
  SendTextMessageInitialData,
  SendTextMessageParams,
  SendTextMessageRequest,
  SendTextMessageResult,
} from "@/components/features/send-text-message/types";

function toParams(params: SendTextMessageParams): string {
  const query = new URLSearchParams();
  if (params.addressId != null)
    query.set("addressId", String(params.addressId));
  if (params.phoneType) query.set("phoneType", params.phoneType);
  if (params.number) query.set("number", params.number);
  const s = query.toString();
  return s ? `?${s}` : "";
}

/** Initial form data: the prefilled receiver, the initial message text and the max message length. */
export function fetchSendTextMessageData(
  params: SendTextMessageParams,
  signal?: AbortSignal
): Promise<SendTextMessageInitialData> {
  return request<SendTextMessageInitialData>(
    `/rs/sendTextMessage${toParams(params)}`,
    { method: "GET" },
    signal
  );
}

/**
 * Receiver suggestions: `"<number>: <name>, ..."` for the matching addresses; an empty search offers the
 * numbers recently sent to.
 */
export function suggestReceivers(
  search: string,
  signal?: AbortSignal
): Promise<string[]> {
  return request<string[]>(
    `/rs/sendTextMessage/ac?search=${encodeURIComponent(search)}`,
    { method: "GET" },
    signal
  );
}

/** Sends the message; the result carries a localized success or error text for the toast. */
export function sendTextMessage(
  body: SendTextMessageRequest,
  signal?: AbortSignal
): Promise<SendTextMessageResult> {
  return request<SendTextMessageResult>(
    "/rs/sendTextMessage/send",
    { method: "POST", body: JSON.stringify(body) },
    signal
  );
}
