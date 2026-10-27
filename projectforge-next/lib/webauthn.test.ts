import { describe, expect, it } from "vitest";
import { bufferToBase64url, decodeBase64url } from "./base64url";
import {
  toPublicKeyCreationOptions,
  toRegisterFinishRequest,
  type WebAuthnRequestOptions,
} from "./webauthn";

const bytes = (...values: number[]) => new Uint8Array(values).buffer;

/** What WebAuthnServicesRest.register answers (base64url values, echoed back by the finish request). */
const options: WebAuthnRequestOptions = {
  rp: { id: "localhost", name: "ProjectForge" },
  user: { id: "AQID", name: "kai", displayName: "Kai" },
  challenge: "-_8",
  timeout: 60000,
  requestId: "request-1",
  pubKeyCredParams: [{ type: "public-key", alg: -7 }],
  excludeCredentials: [{ type: "public-key", id: "BAU" }],
  extensions: {},
};

describe("base64url", () => {
  it("round-trips bytes the url-safe alphabet needs", () => {
    const buffer = bytes(0xfb, 0xff, 0xbf, 0x00);
    expect(bufferToBase64url(buffer)).toBe("-_-_AA");
    expect(new Uint8Array(decodeBase64url("-_-_AA"))).toEqual(
      new Uint8Array(buffer)
    );
  });
});

describe("toPublicKeyCreationOptions", () => {
  it("decodes challenge, user id and the keys to exclude", () => {
    const result = toPublicKeyCreationOptions(options);
    expect(new Uint8Array(result.challenge as ArrayBuffer)).toEqual(
      new Uint8Array(bytes(0xfb, 0xff))
    );
    expect(new Uint8Array(result.user.id as ArrayBuffer)).toEqual(
      new Uint8Array(bytes(1, 2, 3))
    );
    expect(result.excludeCredentials).toHaveLength(1);
    expect(
      new Uint8Array(result.excludeCredentials![0].id as ArrayBuffer)
    ).toEqual(new Uint8Array(bytes(4, 5)));
    expect(result.rp).toEqual(options.rp);
  });
});

describe("toRegisterFinishRequest", () => {
  it("encodes the attestation and echoes request id, challenge and session token", () => {
    const result = toRegisterFinishRequest(
      {
        type: "public-key",
        id: "AQID",
        rawId: bytes(1, 2, 3),
        response: {
          attestationObject: bytes(4, 5),
          clientDataJSON: bytes(6),
          transports: ["usb"],
        },
      },
      options
    );
    expect(result).toEqual({
      requestId: "request-1",
      credential: {
        type: "public-key",
        id: "AQID",
        rawId: "AQID",
        response: {
          attestationObject: "BAU",
          clientDataJSON: "Bg",
          transports: ["usb"],
        },
      },
      clientExtensionResults: {},
      challenge: "-_8",
    });
  });
});
