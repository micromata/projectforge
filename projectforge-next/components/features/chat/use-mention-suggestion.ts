"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { fetchChatMentionCandidates } from "@/lib/rs/chat";
import type { ChatRef } from "@/lib/rs/chat-types";
import type { MentionRender } from "./chat-editor-extensions";
import type { MentionSuggestionState } from "./types";

/**
 * The suggestions of a mention while typing `@…` (MSG-07): the readers of the conversation matching the text
 * after the `@`, from the server (ChatRest.mentionCandidates). Arrow keys move the highlight, Enter or Tab
 * inserts, Escape closes. [state] is null while no list is open; [isOpen] tells the editor not to send on
 * Enter meanwhile.
 */
export function useMentionSuggestion(conversationId: number) {
  const [state, setState] = useState<MentionSuggestionState | null>(null);
  const stateRef = useRef<MentionSuggestionState | null>(null);
  const conversationRef = useRef(conversationId);
  useEffect(() => {
    conversationRef.current = conversationId;
  }, [conversationId]);

  const update = useCallback((next: MentionSuggestionState | null) => {
    stateRef.current = next;
    setState(next);
  }, []);

  const items = useCallback(async (query: string): Promise<ChatRef[]> => {
    try {
      return await fetchChatMentionCandidates(conversationRef.current, query);
    } catch {
      return [];
    }
  }, []);

  const choose = useCallback((item: ChatRef) => {
    stateRef.current?.command({ id: String(item.id), label: item.displayName });
  }, []);

  const render = useCallback(
    (): MentionRender => ({
      onStart: (props) =>
        update({
          items: props.items,
          index: 0,
          rect: props.clientRect?.() ?? null,
          command: props.command,
        }),
      onUpdate: (props) =>
        update({
          items: props.items,
          index: 0,
          rect: props.clientRect?.() ?? null,
          command: props.command,
        }),
      onKeyDown: ({ event }) => {
        const current = stateRef.current;
        if (!current) return false;
        if (event.key === "Escape") {
          update(null);
          return true;
        }
        const count = current.items.length;
        if (event.key === "ArrowDown" || event.key === "ArrowUp") {
          if (count === 0) return true;
          const step = event.key === "ArrowDown" ? 1 : count - 1;
          update({ ...current, index: (current.index + step) % count });
          return true;
        }
        if (event.key === "Enter" || event.key === "Tab") {
          const item = current.items[current.index];
          if (!item) return false;
          choose(item);
          return true;
        }
        return false;
      },
      onExit: () => update(null),
    }),
    [choose, update]
  );

  /** Is a list with suggestions open (Enter chooses one instead of sending)? */
  const isOpen = useCallback(
    () => (stateRef.current?.items.length ?? 0) > 0,
    []
  );

  return { state, items, render, choose, isOpen, close: () => update(null) };
}
