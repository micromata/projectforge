/**
 * Booking time sheets for several days at once (`org.projectforge.rest.TimesheetBookDaysRest`): the
 * dialog's defaults, the vacations a period can be taken from, and the booking with its dry run.
 */

import { rawRequest, request, RsError } from "./client";
import type {
  BookDaysRequest,
  BookDaysResult,
  BookDaysVacations,
} from "@/components/features/timesheet/book-days/types";
import type { ResponseAction } from "./types";

const BASE = "/rs/timesheet/bookDays";
const NOT_ACCEPTABLE = 406;

/** The defaults of the dialog for this user: the first free working day from `from` on, the hours per day. */
export interface BookDaysInitial {
  startDate: string;
  hoursPerDay: number;
}

function query(params: Record<string, string | number | null | undefined>) {
  const search = new URLSearchParams();
  for (const [key, value] of Object.entries(params)) {
    if (value != null) search.set(key, String(value));
  }
  return search.toString();
}

export function fetchBookDaysInitial(
  userId: number | null,
  from: string,
  signal?: AbortSignal
): Promise<BookDaysInitial> {
  return request<BookDaysInitial>(
    `${BASE}/initial?${query({ userId, from })}`,
    { method: "GET" },
    signal
  );
}

export function fetchBookDaysVacations(
  userId: number | null,
  signal?: AbortSignal
): Promise<BookDaysVacations> {
  return request<BookDaysVacations>(
    `${BASE}/vacations?${query({ userId })}`,
    { method: "GET" },
    signal
  );
}

/**
 * The days of the period and what happens on each; books them unless `dryRun`. A refused request
 * (HTTP 406, e.g. no access or a protected period) throws an RsError carrying the server's message —
 * nothing of it is booked then.
 */
export async function postBookDays(
  body: BookDaysRequest,
  signal?: AbortSignal
): Promise<BookDaysResult> {
  const res = await rawRequest(
    BASE,
    { method: "POST", body: JSON.stringify(body) },
    signal
  );
  if (res.status === NOT_ACCEPTABLE) {
    const action = (await res
      .json()
      .catch(() => null)) as ResponseAction | null;
    const message = (action?.validationErrors ?? [])
      .map((error) => error.message)
      .join(" ");
    throw new RsError(res.status, message || res.statusText);
  }
  if (!res.ok) {
    throw new RsError(res.status, `${res.status} ${res.statusText}: ${BASE}`);
  }
  return (await res.json()) as BookDaysResult;
}
