import { describe, expect, it } from "vitest";
import {
  decryptMessage,
  deriveSessionKey,
  encryptMessage,
  exportPublicKey,
  generateSecretKeyPair,
  secretFingerprint,
} from "./secret-crypto";

async function session(id = "session-1") {
  const initiator = await generateSecretKeyPair();
  const recipient = await generateSecretKeyPair();
  const initiatorKey = await exportPublicKey(initiator.publicKey);
  const recipientKey = await exportPublicKey(recipient.publicKey);
  return {
    initiatorKey,
    recipientKey,
    // Each side derives its key from its private and the partner's public key.
    initiatorAes: await deriveSessionKey(
      initiator.privateKey,
      recipientKey,
      id
    ),
    recipientAes: await deriveSessionKey(
      recipient.privateKey,
      initiatorKey,
      id
    ),
  };
}

describe("secret-crypto", () => {
  it("both sides write to each other with the same session key", async () => {
    const { initiatorAes, recipientAes } = await session();
    const toRecipient = await encryptMessage(
      "pässwörd 🔑",
      initiatorAes,
      "session-1",
      "initiator",
      1
    );
    expect(toRecipient.ciphertext).not.toContain("pässwörd");
    await expect(
      decryptMessage(toRecipient, recipientAes, "session-1", "initiator")
    ).resolves.toBe("pässwörd 🔑");
    const reply = await encryptMessage(
      "danke",
      recipientAes,
      "session-1",
      "recipient",
      1
    );
    await expect(
      decryptMessage(reply, initiatorAes, "session-1", "recipient")
    ).resolves.toBe("danke");
  });

  it("rejects another session, a wrong counter, a reflected message and a third party", async () => {
    const { initiatorAes, recipientAes } = await session();
    const message = await encryptMessage(
      "secret",
      initiatorAes,
      "session-1",
      "initiator",
      3
    );
    await expect(
      decryptMessage(message, recipientAes, "session-2", "initiator")
    ).rejects.toBeDefined();
    await expect(
      decryptMessage(
        { ...message, counter: 4 },
        recipientAes,
        "session-1",
        "initiator"
      )
    ).rejects.toBeDefined();
    // Played back to its own sender as if the partner had written it.
    await expect(
      decryptMessage(message, initiatorAes, "session-1", "recipient")
    ).rejects.toBeDefined();
    const other = await session();
    await expect(
      decryptMessage(message, other.recipientAes, "session-1", "initiator")
    ).rejects.toBeDefined();
    const flipped = message.ciphertext.startsWith("A") ? "B" : "A";
    await expect(
      decryptMessage(
        { ...message, ciphertext: flipped + message.ciphertext.slice(1) },
        recipientAes,
        "session-1",
        "initiator"
      )
    ).rejects.toBeDefined();
  });

  it("the private key can't be exported", async () => {
    const pair = await generateSecretKeyPair();
    await expect(
      crypto.subtle.exportKey("pkcs8", pair.privateKey)
    ).rejects.toBeDefined();
  });

  it("the fingerprint covers both keys and is the same on both sides", async () => {
    const { initiatorKey, recipientKey } = await session();
    const fingerprint = await secretFingerprint(initiatorKey, recipientKey);
    expect(fingerprint).toMatch(/^\d{5} \d{5} \d{5} \d{5}$/);
    expect(await secretFingerprint(initiatorKey, recipientKey)).toBe(
      fingerprint
    );
    const swapped = await exportPublicKey(
      (await generateSecretKeyPair()).publicKey
    );
    expect(await secretFingerprint(swapped, recipientKey)).not.toBe(
      fingerprint
    );
    expect(await secretFingerprint(initiatorKey, swapped)).not.toBe(
      fingerprint
    );
  });
});
