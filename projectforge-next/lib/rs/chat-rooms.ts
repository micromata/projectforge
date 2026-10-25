import { chatPost, chatRequest } from "./chat";
import type { ChatRoomDefaults, ChatRoomEntry, ChatRoom } from "./chat-types";

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

/** Defaults of a new room: expiry of its files and the choices, whether files can be exchanged at all. */
export function fetchChatRoomDefaults(signal?: AbortSignal) {
  return chatRequest<ChatRoomDefaults>(
    "/rs/chatRoom/defaults",
    { method: "GET" },
    signal
  );
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
