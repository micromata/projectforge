/** Never more than this many characters of a hidden secret show through. */
const PEEK_MAX = 4;

/**
 * The beginning of a hidden secret that is shown, fading out (SecretInput's and ReadonlyValue's `peek`):
 * a third of it, at most four characters — enough to tell two tokens apart, far too little to guess
 * the rest. A secret of fewer than three characters shows nothing.
 */
export function secretPeek(value: string): string {
  return value.slice(0, Math.min(PEEK_MAX, Math.floor(value.length / 3)));
}
