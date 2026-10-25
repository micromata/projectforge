import { chatPost, chatRequest } from "./chat";
import type { ChatRoomEntry, ChatRoom } from "./chat-types";

export const CHAT_ROOMS_QUERY_KEY = ["chatRooms"] as const;

export function fetchChatRooms(signal?: AbortSignal) {
  return chatRequest<ChatRoomEntry[]>(
    "/rs/chatRoom/list",
    { method: "GET" },
    signal
  );
}

export function fetchChatRoom(id: number, signal?: AbortSignal) {
  return chatRequest<ChatRoom>(`/rs/chatRoom/${id}`, { method: "GET" }, signal);
}

export function saveChatRoom(room: ChatRoom) {
  return chatPost<{ id: number; conversationId: number | null }>(
    "/rs/chatRoom/save",
    room
  );
}

export function deleteChatRoom(id: number) {
  return chatRequest<void>(`/rs/chatRoom/${id}`, { method: "DELETE" });
}
