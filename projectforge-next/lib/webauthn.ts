// WebAuthn (FIDO2) glue between the browser API and ProjectForge's backend.
//
// The Spring endpoints do NOT speak the `webauthn-json` wire format: they send
// base64url strings and expect `requestId`, `challenge` and `sessionToken` to be
// echoed back alongside the credential (see WebAuthnServicesRest.doWebAuthnFinish
// and WebAuthnFinishRequest). So the conversion below mirrors that contract
// instead of using a generic library.

/** Mirrors org.projectforge.security.dto.WebAuthnPublicKeyCredentialCreationOptions. */
export interface WebAuthnRequestOptions {
  rp: { id?: string; name: string };
  user: { id: string; name: string; displayName: string };
  /** base64url */
  challenge: string;
  timeout: number;
  requestId: string;
  sessionToken: string;
  pubKeyCredParams: PublicKeyCredentialParameters[];
  authenticatorSelection?: AuthenticatorSelectionCriteria;
  attestation?: AttestationConveyancePreference;
  extensions?: Record<string, unknown>;
  /** base64url ids */
  allowCredentials?: { type: string; id: string; rawId?: string }[];
}

/** Mirrors org.projectforge.security.dto.WebAuthnFinishRequest. */
export interface WebAuthnFinishRequest {
  requestId: string;
  credential: {
    type: string;
    id: string;
    rawId: string;
    response: {
      authenticatorData: string;
      clientDataJSON: string;
      signature: string;
      userHandle: string | null;
    };
  };
  clientExtensionResults: Record<string, never>;
  challenge: string;
  sessionToken: string;
}

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

/** True if the browser can do WebAuthn at all (http:// origins other than localhost can't). */
export function isWebAuthnSupported(): boolean {
  return (
    typeof window !== "undefined" &&
    !!window.PublicKeyCredential &&
    !!navigator.credentials?.get
  );
}

/** Turns the backend's base64url options into the ArrayBuffers the browser API wants. */
function toPublicKeyOptions(
  options: WebAuthnRequestOptions
): PublicKeyCredentialRequestOptions {
  return {
    challenge: decodeBase64url(options.challenge),
    timeout: options.timeout,
    rpId: options.rp.id,
    userVerification: options.authenticatorSelection?.userVerification,
    allowCredentials: (options.allowCredentials ?? []).map((credential) => ({
      type: "public-key",
      id: decodeBase64url(credential.id),
    })),
  };
}

/**
 * Runs the browser's authentication ceremony and builds the finish request.
 *
 * @returns null if the user aborted or dismissed the prompt (NotAllowedError) —
 * that is a cancellation, not an error to report.
 */
export async function authenticate(
  options: WebAuthnRequestOptions
): Promise<WebAuthnFinishRequest | null> {
  let credential: Credential | null;
  try {
    credential = await navigator.credentials.get({
      publicKey: toPublicKeyOptions(options),
    });
  } catch (error) {
    if (error instanceof DOMException && error.name === "NotAllowedError") {
      return null;
    }
    throw error;
  }
  if (!credential) {
    return null;
  }
  const assertion = credential as PublicKeyCredential;
  const response = assertion.response as AuthenticatorAssertionResponse;
  return {
    requestId: options.requestId,
    credential: {
      type: assertion.type,
      id: assertion.id,
      rawId: bufferToBase64url(assertion.rawId),
      response: {
        authenticatorData: bufferToBase64url(response.authenticatorData),
        clientDataJSON: bufferToBase64url(response.clientDataJSON),
        signature: bufferToBase64url(response.signature),
        userHandle: response.userHandle
          ? bufferToBase64url(response.userHandle)
          : null,
      },
    },
    clientExtensionResults: {},
    challenge: options.challenge,
    sessionToken: options.sessionToken,
  };
}
