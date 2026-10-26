"use client";

import { createContext, useContext } from "react";
import type { ChatConversation, ChatMessage } from "@/lib/rs/chat-types";

/** What the parts of an open conversation share: the conversation and its actions. */
export interface ChatContextValue {
  conversation: ChatConversation;
  /** Fetches the changes since the last known one (after an own action). */
  sync: () => Promise<void>;
  openThread: (rootId: number) => void;
  /** The message a deep link points to (highlighted, scrolled to). */
  highlightId: number | null;
  /**
   * The read state for the "new" lines: as when the conversation was opened (marking read meanwhile doesn't move
   * it), or as marked unread since.
   */
  readMark: number | null;
  /** Has the thread of the message replies of others newer than [readMark], not opened since? */
  hasUnreadReplies: (message: ChatMessage) => boolean;
  /** Marks the conversation unread from the message on. */
  markUnread: (message: ChatMessage) => void;
}

const ChatContext = createContext<ChatContextValue | null>(null);

export const ChatContextProvider = ChatContext.Provider;

export function useChatContext(): ChatContextValue {
  const value = useContext(ChatContext);
  if (!value)
    throw new Error("useChatContext outside of a ChatContextProvider.");
  return value;
}
