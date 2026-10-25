import { rawRequest, RsError } from "./client";
import type { HistoryEntry } from "./history";
import type {
  ChatRef,
  ChatMessage,
  ChatConversation,
  ChatMessagesPage,
  ChatThread,
  ChatChanges,
} from "./chat-types";

/**
 * A refused chat call: 403 without access, 400 with the translated reason (e.g. "The message is empty."),
 * which the UI shows as it is.
 */
export class ChatError extends RsError {}

export async function chatRequest<T>(
  path: string,
  init: RequestInit,
  signal?: AbortSignal
): Promise<T> {
  const res = await rawRequest(path, init, signal);
  if (!res.ok) {
    const body = (await res.json().catch(() => null)) as {
      message?: string;
    } | null;
    throw new ChatError(
      res.status,
      body?.message || `${res.status} ${res.statusText}: ${path}`
    );
  }
  if (res.status === 204) return undefined as T;
  return (await res.json()) as T;
}

export function chatPost<T>(path: string, body: unknown, signal?: AbortSignal) {
  return chatRequest<T>(
    path,
    { method: "POST", body: JSON.stringify(body) },
    signal
  );
}

const BASE = "/rs/chat";

export function chatConversationQueryKey(id: number) {
  return ["chatConversation", id] as const;
}

/** The url of the server-sent events of a conversation (EventSource, same origin, session cookie). */
export function chatEventsUrl(conversationId: number) {
  return `${BASE}/conversation/${conversationId}/events`;
}

/** The url of an image of a conversation; `download` asks for an attachment instead of inline. */
export function chatImageUrl(
  conversationId: number,
  fileId: string,
  download = false
) {
  return `${BASE}/conversation/${conversationId}/image/${encodeURIComponent(fileId)}${download ? "?download=true" : ""}`;
}

export function openChatConversation(
  hostType: string,
  hostId: number,
  channel: string
) {
  return chatPost<{ conversationId: number | null }>(`${BASE}/open`, {
    hostType,
    hostId,
    channel,
  });
}

export function fetchChatConversation(id: number, signal?: AbortSignal) {
  return chatRequest<ChatConversation>(
    `${BASE}/conversation/${id}`,
    { method: "GET" },
    signal
  );
}

export function fetchChatMessages(
  id: number,
  before: number | null,
  signal?: AbortSignal
) {
  const query = before != null ? `?before=${before}` : "";
  return chatRequest<ChatMessagesPage>(
    `${BASE}/conversation/${id}/messages${query}`,
    { method: "GET" },
    signal
  );
}

export function fetchChatThread(
  id: number,
  rootId: number,
  signal?: AbortSignal
) {
  return chatRequest<ChatThread>(
    `${BASE}/conversation/${id}/thread/${rootId}`,
    { method: "GET" },
    signal
  );
}

export function fetchChatChanges(
  id: number,
  since: number,
  signal?: AbortSignal
) {
  return chatRequest<ChatChanges>(
    `${BASE}/conversation/${id}/changes?since=${since}`,
    { method: "GET" },
    signal
  );
}

export function sendChatMessage(
  id: number,
  html: string,
  threadRootId: number | null,
  imageIds: string[]
) {
  return chatPost<ChatMessage>(`${BASE}/conversation/${id}/messages`, {
    html,
    threadRootId,
    imageIds,
  });
}

export function editChatMessage(
  messageId: number,
  html: string,
  imageIds: string[]
) {
  return chatRequest<ChatMessage>(`${BASE}/message/${messageId}`, {
    method: "PUT",
    body: JSON.stringify({ html, imageIds }),
  });
}

export function deleteChatMessage(messageId: number) {
  return chatRequest<void>(`${BASE}/message/${messageId}`, {
    method: "DELETE",
  });
}

export function hideChatMessage(
  messageId: number,
  hidden: boolean,
  reason: string | null
) {
  return chatPost<void>(`${BASE}/message/${messageId}/hide`, {
    hidden,
    reason,
  });
}

export function toggleChatReaction(messageId: number, emoji: string) {
  return chatPost<void>(`${BASE}/message/${messageId}/reaction`, { emoji });
}

export function followChatThread(messageId: number, follow: boolean) {
  return chatPost<void>(`${BASE}/message/${messageId}/follow`, {
    value: follow,
  });
}

export function fetchChatMessageHistory(messageId: number) {
  return chatRequest<HistoryEntry[]>(`${BASE}/message/${messageId}/history`, {
    method: "GET",
  });
}

export function markChatRead(id: number, messageId: number) {
  return chatPost<void>(`${BASE}/conversation/${id}/read`, { messageId });
}

export function muteChatConversation(id: number, muted: boolean) {
  return chatPost<void>(`${BASE}/conversation/${id}/mute`, { value: muted });
}

export function fetchChatMentionCandidates(
  id: number,
  search: string,
  signal?: AbortSignal
) {
  return chatRequest<ChatRef[]>(
    `${BASE}/conversation/${id}/mentionCandidates?search=${encodeURIComponent(search)}`,
    { method: "GET" },
    signal
  );
}

export function searchChatMessages(
  id: number,
  q: string,
  signal?: AbortSignal
) {
  return chatRequest<ChatMessage[]>(
    `${BASE}/conversation/${id}/search?q=${encodeURIComponent(q)}`,
    { method: "GET" },
    signal
  );
}

export function uploadChatImage(id: number, file: File) {
  const body = new FormData();
  body.append("file", file);
  return chatRequest<{ fileId: string }>(`${BASE}/conversation/${id}/image`, {
    method: "POST",
    body,
  });
}
