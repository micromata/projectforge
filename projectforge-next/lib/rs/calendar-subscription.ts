/**
 * The subscription links of ProjectForge's own calendar feeds (`CalendarSubscriptionInfoPageRest.getInfo`):
 * the logged-in user's time sheets, the holidays and the weeks of year. Each url carries the user's
 * personal, encrypted token, hence the security advice that comes with it.
 */

import { request } from "./client";

/** The feeds `GET /rs/calendarSubscription/info?type=` knows; anything else answers the time sheets. */
export type CalendarSubscriptionType =
  | "TIMESHEETS"
  | "HOLIDAYS"
  | "WEEK_OF_YEAR";

/** `CalendarSubscriptionInfo` — the texts are translated by the backend, the urls are absolute. */
export interface CalendarSubscriptionInfo {
  headline?: string;
  url?: string;
  /** Root-relative url of the QR code image; the text to encode goes into its `text` parameter. */
  barcodeUrl?: string;
  securityAdviseHeadline?: string;
  securityAdvise?: string;
}

export function fetchCalendarSubscriptionInfo(
  type: CalendarSubscriptionType,
  signal?: AbortSignal
): Promise<CalendarSubscriptionInfo> {
  return request<CalendarSubscriptionInfo>(
    `/rs/calendarSubscription/info?type=${type}`,
    { method: "GET" },
    signal
  );
}
