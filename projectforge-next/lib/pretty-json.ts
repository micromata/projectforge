/**
 * A JSON text indented by two spaces, one value per line — how a stored, compact JSON value is read and
 * compared line by line (the backend stores it compact). Left as it is if it is no valid JSON (it is
 * being typed, say) or blank; null is "".
 */
export function prettyJson(text: string | null | undefined): string {
  if (text == null) return "";
  if (text.trim().length === 0) return text;
  try {
    return JSON.stringify(JSON.parse(text), null, 2);
  } catch {
    return text;
  }
}

/** Whether the text is valid JSON; a blank one counts as valid (an unset value). */
export function isValidJson(text: string | null | undefined): boolean {
  if (text == null || text.trim().length === 0) return true;
  try {
    JSON.parse(text);
    return true;
  } catch {
    return false;
  }
}
