"use client";

import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { fetchChatUnreadMessages } from "@/lib/rs/chat";

/** Shown with this part of it (or these pixels of a long one), a message counts as seen. */
const SEEN_RATIO = 0.5;
const SEEN_PX = 120;

/**
 * The messages unread when the conversation was opened (or marked unread since: [readMark], [marks] times), also
 * replies in threads, the user hasn't seen yet: a message counts as seen once shown, in the conversation or in its
 * thread ([observe], the ref of a message of others). [pending] are the remaining ones, oldest first; [refresh] after
 * new messages came in.
 */
export function useUnreadMessages(
  conversationId: number,
  readMark: number | null,
  marks: number
) {
  const queryClient = useQueryClient();
  const query = useQuery({
    queryKey: ["chatUnread", conversationId, readMark, marks],
    queryFn: ({ signal }) =>
      fetchChatUnreadMessages(conversationId, readMark ?? 0, signal),
    enabled: readMark != null,
    staleTime: Infinity,
    refetchOnWindowFocus: false,
  });
  const [seen, setSeen] = useState<ReadonlySet<number>>(() => new Set());
  // Marked unread: seen anew from the new read state on, also when it is the same again.
  const mark = `${readMark}:${marks}`;
  const [seenFor, setSeenFor] = useState(mark);
  if (seenFor !== mark) {
    setSeenFor(mark);
    setSeen(new Set());
  }
  const markSeen = useCallback(
    (id: number) =>
      setSeen((ids) => (ids.has(id) ? ids : new Set(ids).add(id))),
    []
  );
  const observer = useRef<IntersectionObserver | null>(null);
  const observed = useRef(new Set<HTMLElement>());
  useEffect(() => () => observer.current?.disconnect(), []);
  // Seen anew: observed again, the observer reports the ones shown right now at once.
  useEffect(() => {
    const current = observer.current;
    if (!current) return;
    observed.current.forEach((element) => {
      current.unobserve(element);
      current.observe(element);
    });
  }, [mark]);

  const observe = useCallback(
    (element: HTMLElement | null) => {
      if (!element) return undefined;
      // The viewport as root: clipped by the scrolling list or thread panel around it.
      observer.current ??= new IntersectionObserver(
        (entries) =>
          entries.forEach((entry) => {
            if (!entry.isIntersecting) return;
            if (
              entry.intersectionRatio < SEEN_RATIO &&
              entry.intersectionRect.height < SEEN_PX
            )
              return;
            const id = Number((entry.target as HTMLElement).dataset.messageId);
            if (id) markSeen(id);
          }),
        { threshold: [0, 0.1, 0.25, SEEN_RATIO] }
      );
      const current = observer.current;
      const elements = observed.current;
      current.observe(element);
      elements.add(element);
      return () => {
        current.unobserve(element);
        elements.delete(element);
      };
    },
    [markSeen]
  );

  const pending = useMemo(
    () => (query.data ?? []).filter((it) => !seen.has(it.id)),
    [query.data, seen]
  );
  const refresh = useCallback(
    () =>
      void queryClient.invalidateQueries({
        queryKey: ["chatUnread", conversationId],
      }),
    [queryClient, conversationId]
  );
  return { pending, observe, markSeen, refresh };
}
