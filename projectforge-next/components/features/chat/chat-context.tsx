"use client";

import { createContext, useContext } from "react";
import type { ChatConversation } from "@/lib/rs/chat-types";

/** What the parts of an open conversation share: the conversation and its actions. */
export interface ChatContextValue {
  conversation: ChatConversation;
  /** Fetches the changes since the last known one (after an own action). */
  sync: () => Promise<void>;
  openThread: (rootId: number) => void;
  /** The message a deep link points to (highlighted, scrolled to). */
  highlightId: number | null;
}

const ChatContext = createContext<ChatContextValue | null>(null);

export const ChatContextProvider = ChatContext.Provider;

export function useChatContext(): ChatContextValue {
  const value = useContext(ChatContext);
  if (!value)
    throw new Error("useChatContext outside of a ChatContextProvider.");
  return value;
}
