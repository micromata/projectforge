// base64url (RFC 4648 §5, without padding), as the Spring endpoints send and expect binary values
// (WebAuthn, the keys and ciphertexts of confidential chat messages).

export function decodeBase64url(value: string): ArrayBuffer {
  const padding = "==".slice(0, (4 - (value.length % 4)) % 4);
  const base64 = value.replace(/-/g, "+").replace(/_/g, "/") + padding;
  const raw = atob(base64);
  const bytes = new Uint8Array(raw.length);
  for (let i = 0; i < raw.length; i += 1) {
    bytes[i] = raw.charCodeAt(i);
  }
  return bytes.buffer;
}

export function bufferToBase64url(buffer: ArrayBuffer): string {
  const bytes = new Uint8Array(buffer);
  let raw = "";
  bytes.forEach((byte) => {
    raw += String.fromCharCode(byte);
  });
  return btoa(raw).replace(/\+/g, "-").replace(/\//g, "_").replace(/=/g, "");
}
