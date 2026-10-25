import type { SecretPayload } from "@/lib/secret-crypto";
import { chatPost, chatRequest } from "./chat";
import type { ChatRef } from "./chat-types";

/**
 * A confidential message of a direct chat as one of its two sides sees it (ChatSecretRest): end-to-end
 * encrypted by the browsers, the server only relays it in memory. Gone (404) once received, cancelled or
 * expired.
 */
export interface ChatSecretSession {
  id: string;
  conversationId: number;
  state: "OFFERED" | "ACCEPTED" | "DELIVERED";
  sender: ChatRef;
  recipient: ChatRef;
  /** Is the logged-in user the sender? */
  mine: boolean;
  /** The recipient's public key (raw P-256, base64url), after accepting. */
  recipientKey: string | null;
  expiresAt: string;
}

const BASE = "/rs/chatSecret";

export function chatSecretQueryKey(id: string) {
  return ["chatSecret", id] as const;
}

export function chatSecretPendingQueryKey(conversationId: number) {
  return ["chatSecretPending", conversationId] as const;
}

export function offerChatSecret(conversationId: number) {
  return chatPost<ChatSecretSession>(`${BASE}/offer`, { conversationId });
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

export function deliverChatSecret(id: string, payload: SecretPayload) {
  return chatPost<ChatSecretSession>(
    `${BASE}/${encodeURIComponent(id)}/deliver`,
    payload
  );
}

/** The ciphertext, only once: the session is deleted with it. */
export function takeChatSecret(id: string) {
  return chatPost<SecretPayload>(`${BASE}/${encodeURIComponent(id)}/take`, {});
}

export function cancelChatSecret(id: string) {
  return chatPost<void>(`${BASE}/${encodeURIComponent(id)}/cancel`, {});
}
