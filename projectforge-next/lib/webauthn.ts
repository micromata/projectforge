// WebAuthn (FIDO2) glue between the browser API and ProjectForge's backend.
//
// The Spring endpoints do NOT speak the `webauthn-json` wire format: they send
// base64url strings and expect `requestId` and `challenge` to be
// echoed back alongside the credential (see WebAuthnServicesRest.doWebAuthnFinish
// and WebAuthnFinishRequest). So the conversion below mirrors that contract
// instead of using a generic library.

import { bufferToBase64url, decodeBase64url } from "./base64url";

/** Mirrors org.projectforge.security.dto.WebAuthnPublicKeyCredentialCreationOptions. */
export interface WebAuthnRequestOptions {
  rp: { id?: string; name: string };
  user: { id: string; name: string; displayName: string };
  /** base64url */
  challenge: string;
  timeout: number;
  requestId: string;
  pubKeyCredParams: PublicKeyCredentialParameters[];
  authenticatorSelection?: AuthenticatorSelectionCriteria;
  attestation?: AttestationConveyancePreference;
  extensions?: Record<string, unknown>;
  /** base64url ids */
  allowCredentials?: { type: string; id: string; rawId?: string }[];
  /** base64url ids of the user's registered keys (registration only), so a key isn't registered twice. */
  excludeCredentials?: { type: string; id: string; rawId?: string }[];
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
}

/** The finish request of a registration (WebAuthnServicesRest.doRegisterFinish), same envelope. */
export interface WebAuthnRegisterFinishRequest {
  requestId: string;
  credential: {
    type: string;
    id: string;
    rawId: string;
    response: {
      attestationObject: string;
      clientDataJSON: string;
      transports: string[];
    };
  };
  clientExtensionResults: Record<string, never>;
  challenge: string;
}

/** True if the browser can do WebAuthn at all (http:// origins other than localhost can't). */
export function isWebAuthnSupported(): boolean {
  return (
    typeof window !== "undefined" &&
    !!window.PublicKeyCredential &&
    !!navigator.credentials?.get
  );
}

/**
 * Turns the backend's base64url options into the ArrayBuffers the browser API wants.
 *
 * `extensions` is left out on purpose: the backend sends the same `appidExclude`/`credProps` as for a
 * registration (`WebAuthnServicesRest.webAuthn`), and both are registration extensions without meaning for
 * `navigator.credentials.get`.
 */
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
  };
}

/** Turns the backend's base64url registration options into the ArrayBuffers the browser API wants. */
export function toPublicKeyCreationOptions(
  options: WebAuthnRequestOptions
): PublicKeyCredentialCreationOptions {
  return {
    rp: options.rp,
    user: {
      id: decodeBase64url(options.user.id),
      name: options.user.name,
      displayName: options.user.displayName,
    },
    challenge: decodeBase64url(options.challenge),
    pubKeyCredParams: options.pubKeyCredParams,
    timeout: options.timeout,
    authenticatorSelection: options.authenticatorSelection,
    attestation: options.attestation,
    extensions: options.extensions as AuthenticationExtensionsClientInputs,
    excludeCredentials: (options.excludeCredentials ?? []).map(
      (credential) => ({
        type: "public-key",
        id: decodeBase64url(credential.id),
      })
    ),
  };
}

/** The attestation of a new key as the backend expects it (all binary values base64url). */
export function toRegisterFinishRequest(
  credential: {
    type: string;
    id: string;
    rawId: ArrayBuffer;
    response: {
      attestationObject: ArrayBuffer;
      clientDataJSON: ArrayBuffer;
      transports: string[];
    };
  },
  options: WebAuthnRequestOptions
): WebAuthnRegisterFinishRequest {
  return {
    requestId: options.requestId,
    credential: {
      type: credential.type,
      id: credential.id,
      rawId: bufferToBase64url(credential.rawId),
      response: {
        attestationObject: bufferToBase64url(
          credential.response.attestationObject
        ),
        clientDataJSON: bufferToBase64url(credential.response.clientDataJSON),
        transports: credential.response.transports,
      },
    },
    clientExtensionResults: {},
    challenge: options.challenge,
  };
}

/**
 * Runs the browser's registration ceremony for a new key and builds the finish request.
 *
 * @returns null if the user aborted or dismissed the prompt (NotAllowedError), as for [authenticate].
 * An already registered key (InvalidStateError, see excludeCredentials) is thrown.
 */
export async function register(
  options: WebAuthnRequestOptions
): Promise<WebAuthnRegisterFinishRequest | null> {
  let credential: Credential | null;
  try {
    credential = await navigator.credentials.create({
      publicKey: toPublicKeyCreationOptions(options),
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
  const attestation = credential as PublicKeyCredential;
  const response = attestation.response as AuthenticatorAttestationResponse;
  return toRegisterFinishRequest(
    {
      type: attestation.type,
      id: attestation.id,
      rawId: attestation.rawId,
      response: {
        attestationObject: response.attestationObject,
        clientDataJSON: response.clientDataJSON,
        // Not offered by every browser; the backend takes none as "unknown".
        transports: response.getTransports?.() ?? [],
      },
    },
    options
  );
}
