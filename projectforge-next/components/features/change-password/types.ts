/** Which password a change password page is about. */
export type PasswordVariant = "password" | "wlan";

/** Mirror of `ChangePasswordInit` (ChangePasswordData.kt): the initial data of the page. */
export interface ChangePasswordInit {
  userId: number;
  userDisplayName?: string | null;
  /** True if the logged-in user changes their own password: the old (login) password is asked then. */
  changeOwn: boolean;
  /** Date and time of the last change with the time ago, e.g. "13.10.2026 11:06 (vor 2 Stunden)". */
  lastChangeFormatted?: string | null;
  /** The rules a new password must follow, already translated. */
  passwordQualityHint?: string | null;
}

/** Mirror of `ChangePasswordData`: what is posted. */
export interface ChangePasswordValues {
  userId: number;
  loginPassword: string;
  newPassword: string;
  passwordRepeat: string;
}

export type ChangePasswordField =
  | "loginPassword"
  | "newPassword"
  | "passwordRepeat";
