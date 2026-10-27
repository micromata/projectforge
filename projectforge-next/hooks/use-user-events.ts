"use client";

import { useEffect } from "react";
import { useQueryClient, type QueryKey } from "@tanstack/react-query";
import { CHAT_SECRET_OFFERS_QUERY_KEY } from "@/lib/rs/chat-secret";
import { USER_EVENTS_URL } from "@/lib/rs/user-events";

/** The queries an event of the user stream (UserEventPublisher) makes stale: the event itself carries nothing. */
const EVENT_QUERIES: Record<string, QueryKey> = {
  secretOffers: CHAT_SECRET_OFFERS_QUERY_KEY,
};

/**
 * A tab hidden this long closes the stream (opened again when shown), like the polling of a chat: an offer must
 * reach a tab in the background, but a forgotten one mustn't keep its session alive for ever.
 */
const HIDDEN_CLOSE_MS = 15 * 60_000;
/** Without a click or key for this long, the stream isn't opened again after the server closed it (hourly). */
const IDLE_MS = 60 * 60_000;
const RETRY_MS = 30_000;
const FAILURES_BEFORE_PAUSE = 3;

/**
 * The server-sent events of the logged-in user, for whatever page is shown (GET /rs/userEvents): each event
 * reloads the queries it stands for. Mounted once for the authenticated area.
 *
 * The stream doesn't keep an idle tab logged in: it is closed after [HIDDEN_CLOSE_MS] in the background and not
 * reopened after [IDLE_MS] without any interaction. Repeated errors pause it until the tab is shown again.
 */
export function useUserEvents() {
  const queryClient = useQueryClient();
  useEffect(() => {
    let source: EventSource | null = null;
    let failures = 0;
    let lastActivity = Date.now();
    let hiddenTimer: ReturnType<typeof setTimeout> | undefined;
    let retryTimer: ReturnType<typeof setTimeout> | undefined;

    const refreshAll = () =>
      Object.values(EVENT_QUERIES).forEach(
        (queryKey) => void queryClient.invalidateQueries({ queryKey })
      );
    const close = () => {
      source?.close();
      source = null;
      clearTimeout(retryTimer);
    };
    const open = () => {
      if (source || document.hidden) return;
      source = new EventSource(USER_EVENTS_URL, { withCredentials: true });
      Object.entries(EVENT_QUERIES).forEach(([name, queryKey]) =>
        source!.addEventListener(
          name,
          () => void queryClient.invalidateQueries({ queryKey })
        )
      );
      source.onopen = () => {
        failures = 0;
        refreshAll(); // What changed while closed announced no event.
      };
      source.onerror = () => {
        // The browser reconnects by itself, unless the request failed (e.g. logged out): closed then.
        close();
        if (++failures >= FAILURES_BEFORE_PAUSE) return; // Again when shown.
        retryTimer = setTimeout(reopen, RETRY_MS);
      };
    };
    const reopen = () => {
      if (Date.now() - lastActivity < IDLE_MS) open();
    };
    const onActivity = () => {
      lastActivity = Date.now();
      if (!source && failures < FAILURES_BEFORE_PAUSE) open();
    };
    const onVisibility = () => {
      clearTimeout(hiddenTimer);
      if (document.hidden) {
        hiddenTimer = setTimeout(close, HIDDEN_CLOSE_MS);
      } else {
        failures = 0;
        lastActivity = Date.now();
        open();
      }
    };

    open();
    document.addEventListener("visibilitychange", onVisibility);
    window.addEventListener("pointerdown", onActivity, true);
    window.addEventListener("keydown", onActivity, true);
    return () => {
      clearTimeout(hiddenTimer);
      close();
      document.removeEventListener("visibilitychange", onVisibility);
      window.removeEventListener("pointerdown", onActivity, true);
      window.removeEventListener("keydown", onActivity, true);
    };
  }, [queryClient]);
}
