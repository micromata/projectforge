// End-to-end encryption of a confidential chat message (ChatSecretRelay), with the browser's WebCrypto
// only: the recipient makes an ECDH P-256 key pair (private key not extractable, in memory only), the
// sender a second one per message; ECDH → HKDF-SHA-256 (info: the session id) → AES-GCM-256. The server
// only ever sees the public keys and the ciphertext.

import { bufferToBase64url, decodeBase64url } from "./base64url";

const CURVE: EcKeyGenParams = { name: "ECDH", namedCurve: "P-256" };
const IV_BYTES = 12;
const INFO_PREFIX = "projectforge-chat-secret:";

/** What the recipient needs besides its private key (all base64url), as sent by ChatSecretRest. */
export interface SecretPayload {
  senderKey: string;
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

async function deriveAesKey(
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

function additionalData(sessionId: string) {
  return new TextEncoder().encode(sessionId);
}

/** Encrypts the text for the recipient's public key, with a fresh key pair of the sender. */
export async function encryptSecret(
  text: string,
  recipientKey: string,
  sessionId: string
): Promise<SecretPayload> {
  const pair = await generateSecretKeyPair();
  const key = await deriveAesKey(pair.privateKey, recipientKey, sessionId);
  const iv = crypto.getRandomValues(new Uint8Array(IV_BYTES));
  const ciphertext = await crypto.subtle.encrypt(
    { name: "AES-GCM", iv, additionalData: additionalData(sessionId) },
    key,
    new TextEncoder().encode(text)
  );
  return {
    senderKey: await exportPublicKey(pair.publicKey),
    iv: bufferToBase64url(iv.buffer),
    ciphertext: bufferToBase64url(ciphertext),
  };
}

/** Decrypts with the recipient's private key; rejects if anything was changed on the way (GCM tag). */
export async function decryptSecret(
  payload: SecretPayload,
  privateKey: CryptoKey,
  sessionId: string
): Promise<string> {
  const key = await deriveAesKey(privateKey, payload.senderKey, sessionId);
  const plain = await crypto.subtle.decrypt(
    {
      name: "AES-GCM",
      iv: decodeBase64url(payload.iv),
      additionalData: additionalData(sessionId),
    },
    key,
    decodeBase64url(payload.ciphertext)
  );
  return new TextDecoder().decode(plain);
}

/**
 * A short fingerprint of the recipient's public key (4 groups of 5 digits): both sides see it before the
 * text is sent, so a key swapped on the way shows as a difference.
 */
export async function secretFingerprint(publicKey: string): Promise<string> {
  const hash = new Uint8Array(
    await crypto.subtle.digest("SHA-256", decodeBase64url(publicKey))
  );
  const groups: string[] = [];
  for (let i = 0; i < 8; i += 2) {
    groups.push(String((hash[i] << 8) | hash[i + 1]).padStart(5, "0"));
  }
  return groups.join(" ");
}
