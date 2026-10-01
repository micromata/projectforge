/**
 * Whether a search list offers the typed term itself as free text (see [EntitySearchFreeText]), and
 * whether it may be picked.
 *
 * - `null`: nothing to offer — nothing typed, or a record is named exactly like the term (case and
 *   surrounding blanks aside), which is the record that was meant.
 * - `tooLong`: offered, but longer than the field takes, so it cannot be picked.
 */
export function freeTextOffer(
  term: string,
  names: readonly string[],
  maxLength?: number
): { text: string; tooLong: boolean } | null {
  const text = term.trim();
  if (!text) return null;
  const lower = text.toLowerCase();
  if (names.some((name) => name.trim().toLowerCase() === lower)) return null;
  return { text, tooLong: maxLength != null && text.length > maxLength };
}
