/**
 * The subscription links of ProjectForge's own calendar feeds (`CalendarSubscriptionInfoRest.getInfo`):
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
  /** The QR code of [url] as data url (the url carries the token, so it is never part of an image url). */
  qrCode?: string;
  securityAdviseHeadline?: string;
  securityAdvise?: string;
  /**
   * The same feed without the reminders of its events — only for a team calendar (`TeamCal.subscriptionInfo`),
   * where [url] includes them.
   */
  urlWithoutExportedReminders?: string;
  /** The QR code of [urlWithoutExportedReminders] as data url. */
  qrCodeWithoutExportedReminders?: string;
  /** Whether the reminders are exported by default (owner, full and read-only access). */
  remindersExportDefaultValue?: boolean;
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
