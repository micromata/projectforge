/** Mirror of `My2FASetupRest.WebAuthnEntryData`. */
export interface My2FAWebAuthnEntry {
  id: number;
  displayName?: string | null;
  createdFormatted?: string | null;
  lastUpdateFormatted?: string | null;
  signCount?: number | null;
}

/** Mirror of `My2FASetupRest.InitData`. */
export interface My2FASetupInit {
  mobilePhone?: string | null;
  /** False: no text messages in this installation, so there is no mobile phone to set up. */
  smsConfigured: boolean;
  /** False: no second factor at all yet (authenticator app, mobile phone or WebAuthn key). */
  userConfigured2FA: boolean;
  authenticatorConfigured: boolean;
  authenticatorKeyCreated?: string | null;
  /** The last 2FA as time ago, e.g. "5 minutes ago"; absent without any. */
  lastSuccessful2FA?: string | null;
  /** True: the last 2FA is recent enough for changes (see fresh2FAMinutes). */
  fresh2FA: boolean;
  fresh2FAMinutes: number;
  webAuthnEntries: My2FAWebAuthnEntry[];
}

/** Mirror of `My2FASetupRest.AuthenticatorKey`. */
export interface My2FAAuthenticatorKey {
  key: string;
  /** The QR code image as data url (`data:image/png;base64,...`), the key is never part of an image url. */
  qrCodeUrl: string;
}
