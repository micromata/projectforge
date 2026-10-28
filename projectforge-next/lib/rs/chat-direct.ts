import { chatPost, chatRequest } from "./chat";
import type { ChatDirectEntry } from "./chat-types";

export const CHAT_DIRECT_QUERY_KEY = ["chatDirect"] as const;

/** The direct messages of the user (with at least one message), the newest first. */
export function fetchChatDirect(signal?: AbortSignal) {
  return chatRequest<ChatDirectEntry[]>(
    "/rs/chatDirect/list",
    { method: "GET" },
    signal
  );
}

/** The conversation with the user (created if missing); refused for users the logged-in one may not see. */
export function openChatDirect(userId: number) {
  return chatPost<{ conversationId: number }>("/rs/chatDirect/open", {
    userId,
  });
}
