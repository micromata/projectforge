/**
 * The digital business cards of the logged-in user (BusinessCardRest, `/rs/myBusinessCards`). There is no user
 * parameter: the backend works on the own cards only.
 */

import { request } from "./client";
import { downloadFile } from "./download";
import { postEntityAction, type EntityWriteResult } from "./entity";
import type {
  BusinessCard,
  BusinessCardPreview,
  MyBusinessCardsInit,
} from "@/components/features/my-business-cards/types";

const BASE = "/rs/myBusinessCards";

export const MY_BUSINESS_CARDS_QUERY_KEY = ["myBusinessCards"] as const;

export function fetchMyBusinessCards(
  signal?: AbortSignal
): Promise<MyBusinessCardsInit> {
  return request<MyBusinessCardsInit>(BASE, { method: "GET" }, signal);
}

/** A new card (not stored) with the data of the given address. */
export function fetchCardFromAddress(
  addressId: number,
  signal?: AbortSignal
): Promise<BusinessCard> {
  return request<BusinessCard>(
    `${BASE}/fromAddress?id=${encodeURIComponent(addressId)}`,
    { method: "GET" },
    signal
  );
}

/** QR code of the card as it is being edited; nothing is stored. */
export function fetchBusinessCardPreview(
  card: BusinessCard,
  signal?: AbortSignal
): Promise<BusinessCardPreview> {
  return request<BusinessCardPreview>(
    `${BASE}/preview`,
    { method: "POST", body: JSON.stringify(card) },
    signal
  );
}

/** Inserts (no id) or updates the own card; HTTP 406 carries the refusal (see lib/rs/entity.ts). */
export function saveBusinessCard(
  card: BusinessCard
): Promise<EntityWriteResult> {
  return postEntityAction("myBusinessCards", "save", card);
}

export function deleteBusinessCard(id: string): Promise<unknown> {
  return request<unknown>(`${BASE}/delete?id=${encodeURIComponent(id)}`, {
    method: "POST",
  });
}

export function downloadBusinessCardVCard(id: string): Promise<void> {
  return downloadFile(`${BASE}/vcf?id=${encodeURIComponent(id)}`);
}
