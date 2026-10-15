import { afterEach, describe, expect, it, vi } from "vitest";
import {
  encryptAttachment,
  interpretWriteBody,
  testAttachmentDecryption,
} from "./attachments";
import * as client from "./client";
import { RsError } from "./client";

/**
 * What a write's answer means — and above all, what it does *not* mean.
 *
 * The case that matters most is the body that isn't a `ResponseAction` at all: a reverse proxy
 * refusing a large upload answers 413 with an HTML page, and a connection cut mid-body leaves no
 * body. Both used to be read as a successful upload of nothing, which removed the progress row and
 * left the user without a single hint that their file never arrived.
 */
const PATH = "/rs/attachments/upload/book/42/attachments";

describe("interpretWriteBody", () => {
  it("takes the new list from an UPDATE action", () => {
    const body = JSON.stringify({
      targetType: "UPDATE",
      variables: {
        data: { attachments: [{ fileId: "abc", name: "invoice.pdf" }] },
      },
    });
    expect(interpretWriteBody(body, 200, PATH)).toEqual({
      kind: "ok",
      attachments: [{ fileId: "abc", name: "invoice.pdf" }],
    });
  });

  it("reads a TOAST as the refusal it is, although the status is 200", () => {
    const body = JSON.stringify({
      targetType: "TOAST",
      message: { color: "danger", message: "File 'a.pdf' already exists." },
    });
    expect(interpretWriteBody(body, 200, PATH)).toEqual({
      kind: "rejected",
      message: "File 'a.pdf' already exists.",
    });
  });

  it("keeps a TOAST that reports a success (testDecryption) out of the refusals", () => {
    const body = JSON.stringify({
      targetType: "TOAST",
      message: { color: "success", message: "Password is correct." },
    });
    expect(interpretWriteBody(body, 200, PATH)).toMatchObject({ kind: "ok" });
  });

  // The regression this file exists for: none of these may pass as a success.
  it.each([
    ["a proxy's HTML error page", "<html><body>413 Request Entity Too Large"],
    ["an empty body from a cut connection", ""],
    ["valid JSON that is not an action", '{"foo":1}'],
    ["a bare JSON value", "null"],
  ])("rejects %s", (_what, body) => {
    expect(() => interpretWriteBody(body, 413, PATH)).toThrow(RsError);
  });

  it("carries the status of the failed answer, so the cause is reportable", () => {
    expect.assertions(2);
    try {
      interpretWriteBody("<html>413", 413, PATH);
    } catch (error) {
      expect(error).toBeInstanceOf(RsError);
      expect((error as RsError).status).toBe(413);
    }
  });
});

/**
 * The password calls answer a wrong or too short password with HTTP 406 — an answer, not an error,
 * so it has to reach the password field instead of a generic "something went wrong".
 */
describe("encryptAttachment / testAttachmentDecryption", () => {
  const REF = { entity: "book", id: 42, fileId: "abc" };

  afterEach(() => {
    vi.restoreAllMocks();
  });

  function answer(status: number, body: unknown) {
    vi.spyOn(client, "rawRequest").mockResolvedValue(
      new Response(JSON.stringify(body), { status })
    );
  }

  it("reads a 406 as the refused password, with the backend's message", async () => {
    answer(406, {
      validationErrors: [
        { fieldId: "attachment.password", message: "At least 6 characters." },
      ],
    });
    await expect(
      encryptAttachment(REF, "123", "ENCRYPTED_AES256")
    ).resolves.toEqual({ kind: "invalid", message: "At least 6 characters." });
  });

  it("sends the password and the chosen mode with the attachment", async () => {
    answer(200, {
      targetType: "CLOSE_MODAL",
      variables: { data: { attachments: [] } },
    });
    await encryptAttachment(REF, "secret123", "ENCRYPTED_STANDARD");
    const init = vi.mocked(client.rawRequest).mock.calls[0][1];
    expect(JSON.parse(init.body as string)).toEqual({
      data: {
        category: "book",
        id: 42,
        fileId: "abc",
        listId: "attachments",
        attachment: {
          fileId: "abc",
          password: "secret123",
          newZipMode: "ENCRYPTED_STANDARD",
        },
      },
    });
  });

  it("takes the new list from a successful encryption", async () => {
    const attachments = [{ fileId: "xyz", name: "a.pdf.zip" }];
    answer(200, {
      targetType: "CLOSE_MODAL",
      variables: { data: { attachments } },
    });
    await expect(
      encryptAttachment(REF, "secret123", "ENCRYPTED_AES256")
    ).resolves.toEqual({ kind: "ok", attachments });
  });

  it("reports a correct password without a list, so the cache isn't emptied", async () => {
    answer(200, {
      targetType: "TOAST",
      message: { color: "success", message: "Password is correct." },
    });
    await expect(testAttachmentDecryption(REF, "secret123")).resolves.toEqual({
      kind: "ok",
    });
  });

  it("reads a 406 of the test as a wrong password", async () => {
    answer(406, {
      validationErrors: [
        { fieldId: "attachment.password", message: "Wrong password." },
      ],
    });
    await expect(testAttachmentDecryption(REF, "nope")).resolves.toEqual({
      kind: "invalid",
      message: "Wrong password.",
    });
  });
});
