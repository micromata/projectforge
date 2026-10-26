import { rawRequest, RsError } from "./client";
import type { HistoryEntry } from "./history";
import type {
  ChatRef,
  ChatMessage,
  ChatConversation,
  ChatMessagesPage,
  ChatThread,
  ChatChanges,
  ChatHostEntry,
  ChatUnreadMessage,
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

/**
 * The url of a file of a conversation: an image inline (thumbnail, lightbox) unless `download`, any other file is
 * always a download.
 */
export function chatFileUrl(
  conversationId: number,
  fileId: string,
  download = false
) {
  return `${BASE}/conversation/${conversationId}/file/${encodeURIComponent(fileId)}${download ? "?download=true" : ""}`;
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

/** The host type of the data transfer areas, each one a chat room (PF-CHAT-DT). */
export const CHAT_HOST_DATA_TRANSFER = "dataTransfer";

/** The channel of the chat of a data transfer area. */
export const CHAT_DATA_TRANSFER_CHANNEL = "main";

export function chatHostListQueryKey(hostType: string) {
  return ["chatHosts", hostType] as const;
}

/** The conversations of the host type with messages the user may read (e.g. of the data transfer areas), newest first. */
export function fetchChatHostConversations(
  hostType: string,
  signal?: AbortSignal
) {
  return chatRequest<ChatHostEntry[]>(
    `${BASE}/list/${encodeURIComponent(hostType)}`,
    { method: "GET" },
    signal
  );
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

/**
 * Sends a message; with files as multipart (the part `message` as JSON, the parts `files`): the files are stored
 * in the data transfer together with the message, not before.
 */
export function sendChatMessage(
  id: number,
  html: string,
  threadRootId: number | null,
  files: File[] = [],
  quotedMessageId: number | null = null
) {
  const path = `${BASE}/conversation/${id}/messages`;
  const request = { html, threadRootId, quotedMessageId };
  if (files.length === 0) {
    return chatPost<ChatMessage>(path, request);
  }
  const body = new FormData();
  body.append(
    "message",
    new Blob([JSON.stringify(request)], {
      type: "application/json",
    })
  );
  files.forEach((file) => body.append("files", file, file.name));
  return chatRequest<ChatMessage>(path, { method: "POST", body });
}

/** Changes the text of an own message, its files stay. */
export function editChatMessage(messageId: number, html: string) {
  return chatRequest<ChatMessage>(`${BASE}/message/${messageId}`, {
    method: "PUT",
    body: JSON.stringify({ html }),
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

/** The messages of others unread since [after] (the read state when opened), oldest first, also in threads. */
export function fetchChatUnreadMessages(
  id: number,
  after: number,
  signal?: AbortSignal
) {
  return chatRequest<ChatUnreadMessage[]>(
    `${BASE}/conversation/${id}/unreadMessages?after=${after}`,
    { method: "GET" },
    signal
  );
}

export function markChatRead(id: number, messageId: number) {
  return chatPost<void>(`${BASE}/conversation/${id}/read`, { messageId });
}

/** The thread of the message was opened: resolves the notifications about mentions in it. */
export function markChatThreadRead(id: number, rootId: number) {
  return chatPost<void>(`${BASE}/conversation/${id}/thread/${rootId}/read`, {});
}

/** Marks the conversation unread from the message on (read up to the messages before it). */
export function markChatUnread(id: number, messageId: number) {
  return chatPost<void>(`${BASE}/conversation/${id}/unread`, { messageId });
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
