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
 * Lets the app show a page in the system browser, with a session of its own (the user logs in there): the page shown
 * now, or [path] (a path of next with its prefix, e.g. `/next/2FASetup`). False if there is no app.
 */
export function openPageInBrowser(path?: string): boolean {
  return postToApp({ pf: "openInBrowser", path: path ?? null });
}
