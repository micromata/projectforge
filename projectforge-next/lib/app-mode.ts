/**
 * The mobile app (projectforge-app) shows these pages in its WebView and marks its user agent with
 * `ProjectForgeApp/<version>`. The web session there comes from the app's token (POST /rs/appAuth/webSession).
 *
 * Only for shaping the UI (e.g. no logout, the app has its own): anybody can send this user agent, so it is never a
 * security check.
 */
export const APP_USER_AGENT_MARKER = "ProjectForgeApp/";

export function isInApp(): boolean {
  return (
    typeof navigator !== "undefined" &&
    navigator.userAgent.includes(APP_USER_AGENT_MARKER)
  );
}

/**
 * The app confirms a second factor in the system browser for these pages (`{"pf":"twoFactor"}`, see app-bridge.ts):
 * an app that can marks its user agent with `(browser2FA)` after its version.
 */
export const APP_BROWSER_2FA_MARKER = "(browser2FA)";

export function appConfirms2FAInBrowser(): boolean {
  return isInApp() && navigator.userAgent.includes(APP_BROWSER_2FA_MARKER);
}

/**
 * The app shows a page in the system browser on request (`{"pf":"openInBrowser"}`, see app-bridge.ts), e.g. the passkey
 * setup, as the WebView can't register passkeys: an app that can marks its user agent with `(openInBrowser)` after its
 * version. (Its menu has the entry for every page anyway.)
 */
export const APP_OPEN_IN_BROWSER_MARKER = "(openInBrowser)";

export function appOpensPageInBrowser(): boolean {
  return isInApp() && navigator.userAgent.includes(APP_OPEN_IN_BROWSER_MARKER);
}
