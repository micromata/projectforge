/**
 * Markers a Zod issue carries instead of a finished message.
 *
 * The texts of both rules need arguments the schema doesn't have: `validation.error.fieldRequired`
 * wants the field's *label* and `validation.error.maxLength` the label and the limit — and the label
 * is only known where the field renders (it is a translation of the metadata's `i18nKey`, chosen by
 * the section). So the schema reports which rule broke and the field turns that into the backend's
 * own wording (see `useFieldErrors` in book-edit-fields).
 *
 * Using the backend's messages rather than Zod's English defaults also means a client side complaint
 * reads exactly like the HTTP 406 the server would have answered with.
 */

export const REQUIRED = "@required";

const MAX_LENGTH_PREFIX = "@maxLength:";

export function maxLengthMarker(maxLength: number): string {
  return `${MAX_LENGTH_PREFIX}${maxLength}`;
}

/**
 * The limit of a [maxLengthMarker], or null for any other message.
 */
export function parseMaxLengthMarker(message: string): number | null {
  if (!message.startsWith(MAX_LENGTH_PREFIX)) return null;
  const value = Number(message.slice(MAX_LENGTH_PREFIX.length));
  return Number.isFinite(value) ? value : null;
}
