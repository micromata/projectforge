"use client";

import { useCallback, useMemo } from "react";

/**
 * The unsent draft of a conversation or thread (MSG-09), kept in the browser's local storage per conversation,
 * thread and user: it survives a reload, but stays on this device.
 */
export function useChatDraft(
  conversationId: number,
  threadRootId: number | null,
  userId: number
) {
  const key = `pf.chat.draft.${userId}.${conversationId}.${threadRootId ?? "main"}`;
  const initial = useMemo(() => {
    if (typeof window === "undefined") return "";
    return window.localStorage.getItem(key) ?? "";
  }, [key]);
  const save = useCallback(
    (html: string) => {
      try {
        if (html) window.localStorage.setItem(key, html);
        else window.localStorage.removeItem(key);
      } catch {
        // Storage full or disabled: the draft is simply not kept.
      }
    },
    [key]
  );
  return { initial, save };
}
