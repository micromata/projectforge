// End-to-end encryption of a confidential conversation (ChatSecretRelay), with the browser's WebCrypto
// only: each side makes an ECDH P-256 key pair per session (private key not extractable, in memory only);
// ECDH → HKDF-SHA-256 (info: the session id) → AES-GCM-256, one key for the session. A message is bound to
// the session, to its sender's role and to its counter (additional data), so a replayed, reordered or
// reflected message fails. The server only ever sees the public keys and the ciphertext.

import { bufferToBase64url, decodeBase64url } from "./base64url";

const CURVE: EcKeyGenParams = { name: "ECDH", namedCurve: "P-256" };
const IV_BYTES = 12;
const INFO_PREFIX = "projectforge-chat-secret:";

/** Who started the session: the initiator offered it, the recipient accepted. */
export type SecretRole = "initiator" | "recipient";

/** An encrypted message as sent by ChatSecretRest (iv and ciphertext base64url). */
export interface SecretMessage {
  counter: number;
  iv: string;
  ciphertext: string;
}

/** WebCrypto exists in secure contexts only (https, localhost). */
export function isSecretCryptoSupported(): boolean {
  return typeof crypto !== "undefined" && !!crypto.subtle;
}

/** A key pair whose private key can't be exported (it never leaves this page). */
export function generateSecretKeyPair(): Promise<CryptoKeyPair> {
  return crypto.subtle.generateKey(CURVE, false, ["deriveBits"]);
}

/** The public key, raw (uncompressed point), base64url. */
export async function exportPublicKey(key: CryptoKey): Promise<string> {
  return bufferToBase64url(await crypto.subtle.exportKey("raw", key));
}

/**
 * The key of the session from the own private and the partner's public key (the same on both sides).
 */
export async function deriveSessionKey(
  privateKey: CryptoKey,
  publicKey: string,
  sessionId: string
): Promise<CryptoKey> {
  const peer = await crypto.subtle.importKey(
    "raw",
    decodeBase64url(publicKey),
    CURVE,
    false,
    []
  );
  const shared = await crypto.subtle.deriveBits(
    { name: "ECDH", public: peer },
    privateKey,
    256
  );
  const hkdf = await crypto.subtle.importKey("raw", shared, "HKDF", false, [
    "deriveKey",
  ]);
  return crypto.subtle.deriveKey(
    {
      name: "HKDF",
      hash: "SHA-256",
      salt: new Uint8Array(0),
      info: new TextEncoder().encode(INFO_PREFIX + sessionId),
    },
    hkdf,
    { name: "AES-GCM", length: 256 },
    false,
    ["encrypt", "decrypt"]
  );
}

function additionalData(sessionId: string, role: SecretRole, counter: number) {
  return new TextEncoder().encode(`${sessionId}|${role}|${counter}`);
}

/** Encrypts the text as message [counter] of [role] (the counter rises per side, starting at 1). */
export async function encryptMessage(
  text: string,
  key: CryptoKey,
  sessionId: string,
  role: SecretRole,
  counter: number
): Promise<SecretMessage> {
  const iv = crypto.getRandomValues(new Uint8Array(IV_BYTES));
  const ciphertext = await crypto.subtle.encrypt(
    {
      name: "AES-GCM",
      iv,
      additionalData: additionalData(sessionId, role, counter),
    },
    key,
    new TextEncoder().encode(text)
  );
  return {
    counter,
    iv: bufferToBase64url(iv.buffer),
    ciphertext: bufferToBase64url(ciphertext),
  };
}

/**
 * Decrypts a message of the partner ([senderRole]); rejects if anything was changed on the way, or if it
 * isn't the partner's (GCM tag).
 */
export async function decryptMessage(
  message: SecretMessage,
  key: CryptoKey,
  sessionId: string,
  senderRole: SecretRole
): Promise<string> {
  const plain = await crypto.subtle.decrypt(
    {
      name: "AES-GCM",
      iv: decodeBase64url(message.iv),
      additionalData: additionalData(sessionId, senderRole, message.counter),
    },
    key,
    decodeBase64url(message.ciphertext)
  );
  return new TextDecoder().decode(plain);
}

/**
 * A short fingerprint of both public keys (4 groups of 5 digits): both sides see it, so a key swapped on
 * the way shows as a difference.
 */
export async function secretFingerprint(
  initiatorKey: string,
  recipientKey: string
): Promise<string> {
  const a = new Uint8Array(decodeBase64url(initiatorKey));
  const b = new Uint8Array(decodeBase64url(recipientKey));
  const both = new Uint8Array(a.length + b.length);
  both.set(a, 0);
  both.set(b, a.length);
  const hash = new Uint8Array(await crypto.subtle.digest("SHA-256", both));
  const groups: string[] = [];
  for (let i = 0; i < 8; i += 2) {
    groups.push(String((hash[i] << 8) | hash[i + 1]).padStart(5, "0"));
  }
  return groups.join(" ");
}
