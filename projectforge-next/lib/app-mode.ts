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
