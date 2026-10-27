import type { SecretMessage } from "@/lib/secret-crypto";
import { chatPost, chatRequest } from "./chat";
import type { ChatRef } from "./chat-types";

/**
 * A confidential conversation of a direct chat as one of its two sides sees it (ChatSecretRest): end-to-end
 * encrypted by the browsers, the server only relays it in memory. Gone (404) when ended or expired.
 */
export interface ChatSecretSession {
  id: string;
  conversationId: number;
  /** OFFERED: waiting for the recipient's key. */
  state: "OFFERED" | "OPEN";
  initiator: ChatRef;
  recipient: ChatRef;
  /** Is the logged-in user the initiator? */
  mine: boolean;
  /** The public keys (raw P-256, base64url), the recipient's after accepting. */
  initiatorKey: string;
  recipientKey: string | null;
  /** Messages waiting for the logged-in user. */
  waiting: number;
  /** The highest counter of the own messages the partner has taken. */
  readCounter: number;
  expiresAt: string;
}

const BASE = "/rs/chatSecret";

export function chatSecretQueryKey(id: string) {
  return ["chatSecret", id] as const;
}

export function chatSecretPendingQueryKey(conversationId: number) {
  return ["chatSecretPending", conversationId] as const;
}

export function offerChatSecret(conversationId: number, publicKey: string) {
  return chatPost<ChatSecretSession>(`${BASE}/offer`, {
    conversationId,
    publicKey,
  });
}

export function fetchChatSecret(id: string, signal?: AbortSignal) {
  return chatRequest<ChatSecretSession>(
    `${BASE}/${encodeURIComponent(id)}`,
    { method: "GET" },
    signal
  );
}

/** The sessions of the conversation the logged-in user is the recipient of. */
export function fetchPendingChatSecrets(
  conversationId: number,
  signal?: AbortSignal
) {
  return chatRequest<ChatSecretSession[]>(
    `${BASE}/pending?conversationId=${conversationId}`,
    { method: "GET" },
    signal
  );
}

export function acceptChatSecret(id: string, publicKey: string) {
  return chatPost<ChatSecretSession>(
    `${BASE}/${encodeURIComponent(id)}/accept`,
    { publicKey }
  );
}

export function sendChatSecret(id: string, message: SecretMessage) {
  return chatPost<ChatSecretSession>(
    `${BASE}/${encodeURIComponent(id)}/send`,
    message
  );
}

/** The messages waiting for the user, only once: they are deleted with it. */
export function takeChatSecret(id: string) {
  return chatPost<SecretMessage[]>(
    `${BASE}/${encodeURIComponent(id)}/take`,
    {}
  );
}

export function cancelChatSecret(id: string) {
  return chatPost<void>(`${BASE}/${encodeURIComponent(id)}/cancel`, {});
}

/** The offers of the conversation are shown in the open chat: their notification is resolved. */
export function markChatSecretsSeen(conversationId: number) {
  return chatPost<void>(`${BASE}/seen`, { conversationId });
}
