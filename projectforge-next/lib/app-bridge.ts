/**
 * Messages of these pages to the app (projectforge-app) that shows them in its WebView: `window.ReactNativeWebView`,
 * json with the kind in `pf`. The app takes them only from a page of its server (WebScreen.onMessage).
 */

type AppBridge = { postMessage(message: string): void };

/** The event the app dispatches in the page with the answer to `{"pf":"twoFactor"}`: `detail.ok`. */
export const APP_2FA_EVENT = "pf-app-2fa";

/** Posts [message] to the app; false if there is no app. */
export function postToApp(message: object): boolean {
  const bridge = (window as { ReactNativeWebView?: AppBridge })
    .ReactNativeWebView;
  if (!bridge) return false;
  bridge.postMessage(JSON.stringify(message));
  return true;
}

/**
 * Lets the app confirm a second factor in the system browser, where WebAuthn works (the WebView of the app can't): it
 * logs in again there (a second factor fresher than [maxAgeMillis]) and opens a new web session, whose cookie these
 * pages then send. True if done; false if cancelled, failed or there is no app.
 */
export function confirm2FAInApp(
  maxAgeMillis?: number | null
): Promise<boolean> {
  return new Promise((resolve) => {
    const onAnswer = (event: Event) => {
      window.removeEventListener(APP_2FA_EVENT, onAnswer);
      resolve((event as CustomEvent<{ ok?: unknown }>).detail?.ok === true);
    };
    window.addEventListener(APP_2FA_EVENT, onAnswer);
    if (!postToApp({ pf: "twoFactor", maxAgeMillis: maxAgeMillis ?? null })) {
      window.removeEventListener(APP_2FA_EVENT, onAnswer);
      resolve(false);
    }
  });
}

/**
 * Lets the app open the passkey setup (`/2FASetup`) in the system browser, where WebAuthn registration works (the
 * WebView of the app can't). The user logs in there; nothing comes back to the page. False if there is no app.
 */
export function openPasskeySetupInBrowser(): boolean {
  return postToApp({ pf: "passkeySetup" });
}
