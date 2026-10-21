/**
 * The calls of the visitor form beside the read and the write (`VisitorbookEntityRest`): the stored visitors
 * matching a new one's name, and the company names in use. The visits are in `visitorbook-entry.ts`.
 */

import { request } from "./client";
import { fetchAutoCompletion } from "./dynamic";

const BASE = "/rs/visitorbook";

/** A stored visitor with the name typed so far (`VisitorbookEntityRest.ExistingVisitor`). */
export interface ExistingVisitor {
  id: number;
  firstname?: string | null;
  lastname?: string | null;
  company?: string | null;
  lastDateOfVisit?: string | null;
  numberOfVisits: number;
}

/**
 * The stored visitors whose first and last name start with the given ones, the latest visit first. Empty below
 * two typed characters.
 */
export function fetchExistingVisitors(
  firstname: string,
  lastname: string,
  signal?: AbortSignal
): Promise<ExistingVisitor[]> {
  const params = new URLSearchParams();
  if (firstname) params.set("firstname", firstname);
  if (lastname) params.set("lastname", lastname);
  return request<ExistingVisitor[]>(
    `${BASE}/existing?${params.toString()}`,
    { method: "GET" },
    signal
  );
}

/** The company names of the stored visitors (`VisitorbookDao.isAutocompletionPropertyEnabled`). */
export function fetchCompanySuggestions(
  search: string,
  signal?: AbortSignal
): Promise<string[]> {
  return fetchAutoCompletion<string>(
    "visitorbook/autocomplete?property=company&search=:search",
    search,
    undefined,
    signal
  );
}
