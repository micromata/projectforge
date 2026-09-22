/**
 * Markers a Zod issue carries instead of a finished message.
 *
 * The texts of both rules need arguments the schema doesn't have: `validation.error.fieldRequired`
 * wants the field's *label* and `validation.error.maxLength` the label and the limit — and the label
 * is only known where the field renders (it is a translation of the metadata's `i18nKey`, chosen by
 * the section). So the schema reports which rule broke and the field turns that into the backend's
 * own wording (see `useFieldErrors` in components/shared/form/).
 *
 * Using the backend's messages rather than Zod's English defaults also means a client side complaint
 * reads exactly like the HTTP 406 the server would have answered with.
 */

export const REQUIRED = "@required";

/** Not a whole number — `validation.error.format.integer`, which takes no argument. */
export const INTEGER = "@integer";

const I18N_PREFIX = "@i18n:";
const MAX_LENGTH_PREFIX = "@maxLength:";
const MIN_PREFIX = "@min:";
const MAX_PREFIX = "@max:";

/**
 * A rule whose wording the bundle already has, complete and without arguments — a time sheet's start
 * that lies after its stop (`timePeriodPanel.startTimeAfterStopTime`).
 *
 * For the cross-field rules a schema can check itself: unlike the rules above, their text needs no
 * label and no limit, so the key *is* the message and naming it is all a `refine` has to do. Still a
 * marker rather than the plain key, so the field can tell a key of ours from the server's already
 * translated message (see `useFieldErrors`) — and from one of Zod's English defaults.
 */
export function i18nMarker(key: string): string {
  return `${I18N_PREFIX}${key}`;
}

/** The bundle key of an [i18nMarker], or null for any other message. */
export function parseI18nMarker(message: string): string | null {
  return message.startsWith(I18N_PREFIX)
    ? message.slice(I18N_PREFIX.length)
    : null;
}

export function maxLengthMarker(maxLength: number): string {
  return `${MAX_LENGTH_PREFIX}${maxLength}`;
}

/** Below the entity's lower bound — `validation.error.range.integerToLow`. */
export function minMarker(min: number): string {
  return `${MIN_PREFIX}${min}`;
}

/** Above the entity's upper bound — `validation.error.range.integerToHigh`. */
export function maxMarker(max: number): string {
  return `${MAX_PREFIX}${max}`;
}

/**
 * The limit of a [maxLengthMarker], or null for any other message.
 */
export function parseMaxLengthMarker(message: string): number | null {
  return parseNumberMarker(message, MAX_LENGTH_PREFIX);
}

/** The bound of a [minMarker], or null for any other message. */
export function parseMinMarker(message: string): number | null {
  return parseNumberMarker(message, MIN_PREFIX);
}

/** The bound of a [maxMarker], or null for any other message. */
export function parseMaxMarker(message: string): number | null {
  return parseNumberMarker(message, MAX_PREFIX);
}

function parseNumberMarker(message: string, prefix: string): number | null {
  if (!message.startsWith(prefix)) return null;
  const value = Number(message.slice(prefix.length));
  return Number.isFinite(value) ? value : null;
}
