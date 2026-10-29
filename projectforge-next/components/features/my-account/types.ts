import type { SelectOption } from "@/lib/validation/from-metadata";

/** Mirror of `MyAccountRest.Token`: an access token of the user. */
export type MyAccountTokenType = "CALENDAR_REST" | "DAV_TOKEN" | "REST_CLIENT";

export interface MyAccountToken {
  type: MyAccountTokenType;
  token?: string | null;
  creationDateFormatted?: string | null;
}

/** Mirror of `MyAccountRest.Device`: a device with a valid stay-logged-in token. */
export interface MyAccountDevice {
  id: number;
  userAgent?: string | null;
  lastAccessIp?: string | null;
  lastAccessFormatted?: string | null;
  createdFormatted?: string | null;
  /** The device this page was loaded on. */
  current: boolean;
}

/**
 * Mirror of `MyAccountRest.MyAccountData`. Spring omits null values (`NON_NULL`), hence the optional
 * fields. Only [MyAccountValues] is posted back, the rest is for display.
 */
export interface MyAccountData {
  firstname?: string | null;
  lastname?: string | null;
  locale?: string | null;
  dateFormat?: string | null;
  excelDateFormat?: string | null;
  timeNotation?: string | null;
  timeZone?: string | null;
  personalPhoneIdentifiers?: string | null;
  gpgPublicKey?: string | null;
  sshPublicKey?: string | null;
  username?: string | null;
  mobilePhone?: string | null;
  lastLoginFormatted?: string | null;
  lastPasswordChangeFormatted?: string | null;
  lastWlanPasswordChangeFormatted?: string | null;
  /** False if the WLAN password can't be changed in this installation (no LDAP master). */
  wlanPasswordChangeSupported: boolean;
  groups?: string | null;
  tokens?: MyAccountToken[];
  devices?: MyAccountDevice[];
}

/** Mirror of `MyAccountRest.InitData`. */
export interface MyAccountInit {
  data: MyAccountData;
  /** False for a restricted or a demo user: nothing to save and no token to renew. */
  writeAccess: boolean;
  options: {
    locales: SelectOption[];
    dateFormats: SelectOption[];
    excelDateFormats: SelectOption[];
    timeNotations: SelectOption[];
  };
  /** What applies while a format is empty (the installation's defaults). */
  defaults: {
    dateFormat: string;
    excelDateFormat: string;
    timeNotation?: string | null;
  };
  /** False without a configured Sipgate: no personal phone identifiers then. */
  phoneIdentifiersSupported: boolean;
  /** Menu urls (`next/…`) of the password pages and of the 2FA setup; absent if the menu hides them. */
  changePasswordUrl?: string | null;
  changeWlanPasswordUrl?: string | null;
  my2FASetupUrl?: string | null;
  /** The own id and the version of the avatar, null without one (see UserAvatarField). */
  userId?: number | null;
  avatarVersion?: number | null;
}

/** Mirror of `MyAccountRest.TokenAccess`. */
export interface MyAccountTokenAccess {
  userAgent?: string | null;
  ip?: string | null;
  lastAccessFormatted?: string | null;
  counter: number;
}

/** Mirror of `MyAccountRest.TokenInfo`. */
export interface MyAccountTokenInfo {
  type: MyAccountTokenType;
  accesses?: MyAccountTokenAccess[];
  /** REST client token only: the QR code for setting up a client app (data url). */
  qrCodeUrl?: string | null;
}
