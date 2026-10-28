import { fetchAutoCompletion } from "./dynamic";

/** The two mail books of the organization menu: the inbox (Posteingang) and the outbox (Postausgang). */
export type PostEntity = "incomingMail" | "outgoingMail";

/**
 * The values the stored entries of a mail book already have for a free-text property — the generic
 * property autocompletion, which `PosteingangDao` / `PostausgangDao.isAutocompletionPropertyEnabled`
 * opt the properties into.
 */
export function fetchPostSuggestions(
  entity: PostEntity,
  property: string,
  search: string,
  signal?: AbortSignal
): Promise<string[]> {
  return fetchAutoCompletion<string>(
    `${entity}/autocomplete?property=${property}&search=:search`,
    search,
    undefined,
    signal
  );
}
