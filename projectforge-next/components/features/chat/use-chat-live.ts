"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { chatEventsUrl, fetchChatChanges } from "@/lib/rs/chat";
import type { ChatMessage } from "@/lib/rs/chat-types";
import { applyChatChanges } from "./chat-cache";

const SSE_FAILURES_BEFORE_POLLING = 3;
/**
 * The server sends an event at once on connecting; none within this time means a proxy buffers the stream (the
 * connection is open, but nothing arrives): polling then.
 */
const SSE_FIRST_EVENT_MS = 10_000;
const POLL_VISIBLE_MS = 3_000;
const POLL_HIDDEN_MS = 30_000;
/** A tab hidden that long stops asking: otherwise an idle tab would keep its session alive forever. */
const POLL_HIDDEN_MAX_MS = 15 * 60_000;
/**
 * A tab hidden that long closes the event stream (opened again when shown): the server counts an open stream as
 * watching the conversation and notifies nobody watching, so a forgotten tab mustn't swallow the notifications.
 */
const SSE_HIDDEN_CLOSE_MS = 30_000;
/** In polling mode, server-sent events are tried again after this. */
const SSE_RETRY_MS = 5 * 60_000;

/**
 * Keeps the open conversation up to date (NOT-06): server-sent events announce a change (its sequence), then
 * the changes since the last known sequence are fetched and merged into the cache. After repeated failures of
 * the event stream it falls back to polling (every 3 s while the tab is visible, every 30 s in the background,
 * not at all after 15 minutes in the background) and tries the events again from time to time. The event stream
 * is closed while the tab is hidden for a while; shown again, it reconnects and fetches what was missed.
 *
 * [initialSeq] is the sequence of the conversation when loaded; nothing happens before it is known.
 * [onSecret] is called when a confidential message of the user may have changed (its own event; in polling
 * mode with every poll).
 */
export function useChatLive(
  conversationId: number,
  initialSeq: number | undefined,
  onChanges?: (messages: ChatMessage[]) => void,
  onSecret?: () => void
) {
  const queryClient = useQueryClient();
  const [mode, setMode] = useState<"events" | "polling">("events");
  const shown = useShown();
  const seqRef = useRef<number | null>(null);
  const syncing = useRef(false);
  const again = useRef(false);
  const onChangesRef = useRef(onChanges);
  const onSecretRef = useRef(onSecret);
  useEffect(() => {
    onChangesRef.current = onChanges;
    onSecretRef.current = onSecret;
  }, [onChanges, onSecret]);
  // Before the effect of the events below: they need it.
  useEffect(() => {
    if (seqRef.current == null && initialSeq != null)
      seqRef.current = initialSeq;
  }, [initialSeq]);

  const sync = useCallback(async (): Promise<void> => {
    if (seqRef.current == null) return;
    if (syncing.current) {
      again.current = true; // The running one asks once more when done.
      return;
    }
    syncing.current = true;
    try {
      do {
        again.current = false;
        let complete = false;
        while (!complete) {
          const since = seqRef.current ?? 0;
          const changes = await fetchChatChanges(conversationId, since);
          applyChatChanges(queryClient, conversationId, changes.messages);
          seqRef.current = Math.max(since, changes.changeSeq);
          complete = changes.complete;
          if (changes.messages.length > 0)
            onChangesRef.current?.(changes.messages);
        }
      } while (again.current);
    } catch {
      // The next event or poll tries again.
    } finally {
      syncing.current = false;
    }
  }, [conversationId, queryClient]);

  useEffect(() => {
    if (mode !== "events" || initialSeq == null || !shown) return;
    const source = new EventSource(chatEventsUrl(conversationId), {
      withCredentials: true,
    });
    let failures = 0;
    const buffered = setTimeout(() => {
      source.close();
      setMode("polling");
    }, SSE_FIRST_EVENT_MS);
    source.addEventListener("change", (event) => {
      clearTimeout(buffered);
      const seq = Number(
        (JSON.parse((event as MessageEvent).data) as { changeSeq: number })
          .changeSeq
      );
      if (seqRef.current == null || seq > seqRef.current) void sync();
    });
    source.addEventListener("secret", () => onSecretRef.current?.());
    source.onopen = () => {
      failures = 0;
      onSecretRef.current?.(); // Offers made while closed (hidden tab) announced no event here.
    };
    source.onerror = () => {
      if (++failures >= SSE_FAILURES_BEFORE_POLLING) {
        source.close();
        setMode("polling");
      }
    };
    return () => {
      clearTimeout(buffered);
      source.close();
    };
  }, [conversationId, initialSeq, mode, shown, sync]);

  useEffect(() => {
    if (mode !== "polling") return;
    let hiddenSince: number | null = document.hidden ? Date.now() : null;
    let timer: ReturnType<typeof setTimeout>;
    const schedule = () => {
      timer = setTimeout(
        tick,
        document.hidden ? POLL_HIDDEN_MS : POLL_VISIBLE_MS
      );
    };
    const tick = () => {
      if (!hiddenSince || Date.now() - hiddenSince < POLL_HIDDEN_MAX_MS) {
        void sync();
        onSecretRef.current?.();
      }
      schedule();
    };
    const onVisibility = () => {
      hiddenSince = document.hidden ? Date.now() : null;
      if (!document.hidden) void sync();
    };
    document.addEventListener("visibilitychange", onVisibility);
    schedule();
    const retry = setTimeout(() => setMode("events"), SSE_RETRY_MS);
    return () => {
      clearTimeout(timer);
      clearTimeout(retry);
      document.removeEventListener("visibilitychange", onVisibility);
    };
  }, [mode, sync]);

  return { mode, sync };
}

/** False once the tab is hidden for [SSE_HIDDEN_CLOSE_MS], true again when shown. */
function useShown() {
  const [shown, setShown] = useState(true);
  useEffect(() => {
    let timer: ReturnType<typeof setTimeout> | undefined;
    const onVisibility = () => {
      clearTimeout(timer);
      if (document.hidden)
        timer = setTimeout(() => setShown(false), SSE_HIDDEN_CLOSE_MS);
      else setShown(true);
    };
    onVisibility();
    document.addEventListener("visibilitychange", onVisibility);
    return () => {
      clearTimeout(timer);
      document.removeEventListener("visibilitychange", onVisibility);
    };
  }, []);
  return shown;
}
