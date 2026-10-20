/**
 * The calls of the skill matrix besides the standard REST ones (`SkillEntryEntityRest`): the suggestions of
 * the skill field, and the Excel export of the statistics over all skills (`SkillMatrixServicesRest`).
 */

import { downloadFile } from "./download";
import { fetchAutoCompletion } from "./dynamic";

/** The skills already entered by anybody (`SkillEntryDao.isAutocompletionPropertyEnabled`). */
export function fetchSkillSuggestions(
  search: string,
  signal?: AbortSignal
): Promise<string[]> {
  return fetchAutoCompletion<string>(
    "skillentry/autocomplete?property=skill&search=:search",
    search,
    undefined,
    signal
  );
}

/**
 * Every skill with the number of users naming it and the means of their ratings and interests, the own
 * skills marked — the "export as Excel" of the legacy list. Not the rows of the list: the export is the
 * same whatever the list shows.
 */
export function downloadSkillMatrixExcel(): Promise<void> {
  return downloadFile("/rs/skillmatrix/exportAsExcel");
}
