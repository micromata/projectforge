/**
 * The calls of the address pages beside the list, the read and the write (`AddressEntityRest`,
 * `AddressServicesRest`, `AddressImageServicesRest`, `CardDAVInfoRest`): the view, the favorite flag, the
 * duplicates, the image, the vCard exports, the text parser and the CardDAV access data.
 *
 * The image of the form travels through the session: an upload is kept there until the next save stores it
 * (`AddressSaveSupport`), so a new address can get one before it has an id.
 */

import { rawRequest, request, RsError } from "./client";
import { downloadFile } from "./download";
import { fetchAutoCompletion } from "./dynamic";
import { uploadWithProgress, type UploadOptions } from "./upload";
import type { ResponseAction } from "./types";

/** What the view of an address shows besides the address (`AddressEntityRest.AddressView`). */
export interface AddressViewData<A> {
  address: A;
  phoneCallEnabled: boolean;
  smsEnabled: boolean;
}

/** An address with the same name, for the warning while adding one (`AddressEntityRest.DuplicateAddress`). */
export interface DuplicateAddress {
  id: number;
  fullName?: string | null;
  organization?: string | null;
  email?: string | null;
}

/** A language as `LanguageService.Language` answers it: the locale (`de_DE`) and its name. */
export interface AddressLanguage {
  value: string;
  label: string;
}

/** One field the text parser or a vCard offers, `AddressTextImportService` / `parseVcf`. */
export interface ParsedAddressField {
  value: string;
  confidence?: string | null;
  currentValue?: string | null;
}

export interface ParsedAddressData {
  fields: Record<string, ParsedAddressField>;
  warnings?: string[];
}

/** What `parseVcf` answers: the fields of the matching vCard, or the (translated) reason it failed. */
export interface ParsedVcf {
  parsedData?: ParsedAddressData;
  hasImage?: boolean;
  imageTooLarge?: boolean;
  error?: string;
}

/** The access data a CardDAV client needs (`CardDAVInfoRest.Data`). */
export interface CardDavInfo {
  user?: string;
  password?: string;
  standardUrl?: string;
  appleUrl?: string;
  applePath?: string;
  iOSUrl?: string;
}

const BASE = "/rs/address";

/** Url of the stored image of an address, or of its preview (the list's thumbnail). */
export function addressImageUrl(id: number, preview = false): string {
  return `${BASE}/${preview ? "imagePreview" : "image"}/${id}`;
}

/**
 * Url of the image uploaded to the session and not saved yet, with a version, so a new upload is fetched anew
 * rather than taken from the browser's cache.
 */
export function uploadedAddressImageUrl(version: number): string {
  return `${BASE}/uploadedImage?v=${version}`;
}

/** Url of the image of the last parsed vCard, staged in the session (see [parseVcf]). */
export const VCF_IMAGE_PREVIEW_URL = `${BASE}/vcfImagePreview`;

export function addressViewQueryKey(id: number) {
  return ["address", "view", id] as const;
}

export function fetchAddressView<A>(
  id: number,
  signal?: AbortSignal
): Promise<AddressViewData<A>> {
  return request<AddressViewData<A>>(
    `${BASE}/view/${id}`,
    { method: "GET" },
    signal
  );
}

/** Marks the address as a favorite of the logged-in user, or removes the mark; the address isn't saved. */
export function setAddressFavorite(
  id: number,
  favorite: boolean
): Promise<{ isFavoriteCard: boolean }> {
  return request(`${BASE}/favorite/${id}?favorite=${favorite}`, {
    method: "POST",
  });
}

export function fetchDuplicateAddresses(
  name: string | null | undefined,
  firstName: string | null | undefined,
  signal?: AbortSignal
): Promise<DuplicateAddress[]> {
  const params = new URLSearchParams();
  if (name) params.set("name", name);
  if (firstName) params.set("firstName", firstName);
  return request<DuplicateAddress[]>(
    `${BASE}/duplicates?${params.toString()}`,
    { method: "GET" },
    signal
  );
}

/** The languages of the address book first (`usedLanguages`), then every other one (`acLang`). */
export async function fetchAddressLanguages(
  signal?: AbortSignal
): Promise<{ used: AddressLanguage[]; all: AddressLanguage[] }> {
  const [used, all] = await Promise.all([
    request<AddressLanguage[]>(
      `${BASE}/usedLanguages`,
      { method: "GET" },
      signal
    ),
    request<{ id: string; displayName: string }[]>(
      `${BASE}/acLang?search=`,
      { method: "GET" },
      signal
    ),
  ]);
  return {
    used,
    all: all.map((lang) => ({ value: lang.id, label: lang.displayName })),
  };
}

/** The values other addresses already have for a text field (`AddressDao.isAutocompletionPropertyEnabled`). */
export function fetchAddressSuggestions(
  property: string,
  search: string,
  signal?: AbortSignal
): Promise<string[]> {
  return fetchAutoCompletion<string>(
    `address/autocomplete?property=${property}&search=:search`,
    search,
    undefined,
    signal
  );
}

/**
 * Uploads an image for the form; the next save stores it. A refusal (unsupported type, too large) is
 * thrown with the backend's text.
 */
export async function uploadAddressImage(
  file: File,
  options: UploadOptions = {}
): Promise<void> {
  const body = new FormData();
  body.append("file", file);
  // The id is ignored: the image waits in the session for the save, also for an address that is new.
  const path = `${BASE}/uploadImage/-1`;
  const res = await uploadWithProgress(path, body, options);
  if (res.status < 200 || res.status >= 300) {
    throw new RsError(res.status, res.text || `${res.status}: ${path}`);
  }
}

/**
 * Drops an image uploaded but not saved yet. Never with the id of a stored address: that would delete its
 * image at once, before the form is saved or cancelled — removing it is the save's job (`imageDeleted`).
 */
export async function discardUploadedAddressImage(): Promise<void> {
  const path = `${BASE}/deleteImage/-1`;
  const res = await rawRequest(path, { method: "DELETE" });
  if (!res.ok) throw new RsError(res.status, `${res.status}: ${path}`);
}

/** Takes the image of the last parsed vCard over as the uploaded image of the form. */
export async function applyVcfImage(): Promise<void> {
  const path = `${BASE}/applyVcfImage`;
  const res = await rawRequest(path, { method: "POST" });
  if (!res.ok) throw new RsError(res.status, `${res.status}: ${path}`);
}

export async function parseAddressText(
  inputText: string,
  signal?: AbortSignal
): Promise<ParsedAddressData | undefined> {
  const action = await request<ResponseAction>(
    `${BASE}/parseText`,
    { method: "POST", body: JSON.stringify({ data: { inputText } }) },
    signal
  );
  return action.variables?.parsedData as ParsedAddressData | undefined;
}

/**
 * Parses a VCF file and answers the fields of the vCard matching the address of the form best (any, for a new
 * one). Its image, if any, is staged in the session for [VCF_IMAGE_PREVIEW_URL] and [applyVcfImage].
 */
export async function parseVcfFile(
  file: File,
  addressId: number | null
): Promise<ParsedVcf> {
  const body = new FormData();
  body.append("file", file);
  body.append("addressId", String(addressId ?? 0));
  const action = await request<ResponseAction>(`${BASE}/parseVcf`, {
    method: "POST",
    body,
  });
  return (action.variables ?? {}) as ParsedVcf;
}

/**
 * Applies the picked fields to the address and answers it: the backend converts what a text can't be posted
 * as (the birthday, the form of address, the language).
 */
export async function applyParsedAddressData<A>(
  address: A,
  selectedFields: Record<string, string>
): Promise<A | undefined> {
  const action = await request<ResponseAction>(`${BASE}/applyParsedData`, {
    method: "POST",
    body: JSON.stringify({ address, selectedFields, applyImage: false }),
  });
  return action.variables?.data as A | undefined;
}

export function downloadAddressVCard(id: number): Promise<void> {
  return downloadFile(`${BASE}/exportVCard/${id}`);
}

export function downloadFavoritesVCards(): Promise<void> {
  return downloadFile(`${BASE}/exportFavoritesVCards`);
}

export function downloadFavoritesExcel(): Promise<void> {
  return downloadFile(`${BASE}/exportFavoritesExcel`);
}

export function downloadAppleScript(): Promise<void> {
  return downloadFile(`${BASE}/downloadAppleScript`);
}

export const CARD_DAV_INFO_QUERY_KEY = ["address", "cardDavInfo"] as const;

export function fetchCardDavInfo(signal?: AbortSignal): Promise<CardDavInfo> {
  return request<CardDavInfo>(
    "/rs/cardDAVInfo/info",
    { method: "GET" },
    signal
  );
}
