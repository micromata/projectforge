"use client";

import { useEffect, useRef, type RefObject } from "react";
import { useSearchParams } from "next/navigation";
import type { CalendarApi } from "@fullcalendar/core";

/**
 * Honours `?gotoDate` and `?hash` after a save sends the user back here (see
 * `CalendarServicesRest.redirectToCalendarWithDate`).
 *
 * A `gotoDate` outside the visible range calls `api.gotoDate`, which fires `datesSet` → a new events
 * key → a refetch. A `gotoDate` already in view (or a bare `hash` change) only bumps the nonce, which
 * refetches without moving. The last pair is compared against a ref, not a snapshot of
 * `location.search`, so the same date arriving twice is a no-op instead of a second jump.
 */
export function useGotoDate(
  apiRef: RefObject<CalendarApi | null>,
  bumpNonce: () => void
) {
  const searchParams = useSearchParams();
  const gotoDate = searchParams.get("gotoDate");
  const hash = searchParams.get("hash");
  const last = useRef<{ gotoDate: string | null; hash: string | null }>({
    gotoDate: null,
    hash: null,
  });

  useEffect(() => {
    const api = apiRef.current;
    if (!api) return;
    if (gotoDate && gotoDate !== last.current.gotoDate) {
      const target = new Date(gotoDate);
      if (!Number.isNaN(target.getTime())) {
        const { activeStart, activeEnd } = api.view;
        if (target < activeStart || target >= activeEnd) api.gotoDate(target);
        else bumpNonce();
      }
    } else if (hash && hash !== last.current.hash) {
      bumpNonce();
    }
    last.current = { gotoDate, hash };
  }, [gotoDate, hash, apiRef, bumpNonce]);
}
