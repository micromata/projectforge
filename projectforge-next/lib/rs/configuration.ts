/**
 * Rest calls specific to the system-configuration page (org.projectforge.rest.ConfigurationEntityRest).
 *
 * The list and edit go through the generic entity client (`fetchList`/`fetchOne`/`save`); the only
 * thing special here is the time-zone picker of a `TIME_ZONE` parameter, which completes itself from
 * the same endpoint the legacy form used.
 */

import { fetchAutoCompletion } from "./dynamic";

/** A time zone as `TimeZoneServicesRest.DisplayTimeZone` carries it — the id to store, a display name. */
interface DisplayTimeZone {
  id: string;
  displayName: string;
}

/** React Query key of the time-zone suggestions (`TimeZoneServicesRest`, /rs/timeZones/ac). */
export const TIME_ZONE_SUGGEST_QUERY_KEY = ["timeZones", "ac"] as const;

/**
 * The available time-zone ids, filtered by `search` — bound to the `stringValue` of a `TIME_ZONE`
 * parameter (the `timezone` configuration). The value stored is the id itself ("Europe/Berlin"), so
 * the `DisplayTimeZone`s the backend answers are mapped down to their ids; the display name only
 * disambiguates while typing and is not part of the value.
 */
export function fetchTimeZoneSuggestions(
  search: string,
  signal?: AbortSignal
): Promise<string[]> {
  return fetchAutoCompletion<DisplayTimeZone>(
    "timeZones/ac?search=:search",
    search,
    undefined,
    signal
  ).then((zones) => zones.map((zone) => zone.id));
}
