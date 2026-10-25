import { describe, expect, it } from "vitest";
import {
  decryptSecret,
  encryptSecret,
  exportPublicKey,
  generateSecretKeyPair,
  secretFingerprint,
} from "./secret-crypto";

describe("secret-crypto", () => {
  it("decrypts only with the recipient's key and the same session", async () => {
    const recipient = await generateSecretKeyPair();
    const publicKey = await exportPublicKey(recipient.publicKey);
    const payload = await encryptSecret("pässwörd 🔑", publicKey, "session-1");
    expect(payload.ciphertext).not.toContain("pässwörd");
    await expect(
      decryptSecret(payload, recipient.privateKey, "session-1")
    ).resolves.toBe("pässwörd 🔑");
    await expect(
      decryptSecret(payload, recipient.privateKey, "session-2")
    ).rejects.toBeDefined();
    const other = await generateSecretKeyPair();
    await expect(
      decryptSecret(payload, other.privateKey, "session-1")
    ).rejects.toBeDefined();
  });

  it("the private key can't be exported, the fingerprint is stable", async () => {
    const pair = await generateSecretKeyPair();
    await expect(
      crypto.subtle.exportKey("pkcs8", pair.privateKey)
    ).rejects.toBeDefined();
    const publicKey = await exportPublicKey(pair.publicKey);
    const fingerprint = await secretFingerprint(publicKey);
    expect(fingerprint).toMatch(/^\d{5} \d{5} \d{5} \d{5}$/);
    expect(await secretFingerprint(publicKey)).toBe(fingerprint);
  });
});
