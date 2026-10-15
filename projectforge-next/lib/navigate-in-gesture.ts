import { BASE_PATH } from "@/lib/config";

/** The part of Next's router [navigateInGesture] needs, so a test can hand in a stub. */
export interface GestureRouter {
  push(href: string, options?: { scroll?: boolean }): void;
  replace(href: string, options?: { scroll?: boolean }): void;
}

/**
 * A client-side navigation to [href] (an app route without the base path, as `router.push` takes it)
 * whose history entry is created synchronously, inside the click or key press that asked for it.
 *
 * Why not `router.push`: Next writes the entry only when the navigation commits, ~20 ms after the
 * click. Safari marks an entry that `pushState` adds outside a user gesture and without a recent user
 * activation as "added by JS without user interaction" (`HistoryController::pushState`), and its back
 * button skips such entries (`WebBackForwardList::itemStartingAtIndexSkippingItemsAddedByJSWithoutUserGesture`)
 * — when the current entry is marked, one more besides. Safari 27 hands out no activation for some real
 * clicks, e.g. after a text field had the focus (the list's search field, the main menu's search). Back
 * from an entry then skipped the list and landed on whatever page came before it.
 *
 * So the entry is pushed here, while the gesture is still being processed, and Next is asked to
 * `replace` into it: its commit rewrites the entry with `replaceState`, which Safari does not mark. The
 * pushed state is the current one, so it carries Next's `__NA` flag and Next's patched `pushState` passes
 * it through untouched. Should Next fall back to a full page load, it does that with `location.replace`,
 * which takes the entry pushed here as well — either way it is one entry per navigation.
 *
 * Only for navigations started directly by the user: after an `await` (a confirm dialog, a save) the
 * gesture is over, and pushing here would buy nothing.
 */
export function navigateInGesture(
  router: GestureRouter,
  href: string,
  options?: { scroll?: boolean }
): void {
  // A relative href would have to be resolved first; every caller passes an absolute route.
  if (!href.startsWith("/")) {
    router.push(href, options);
    return;
  }
  // The native API needs the base path the router adds on its own.
  const url = BASE_PATH + href;
  const { pathname, search, hash } = window.location;
  // Compared without the trailing slash the static export adds to every path: a link to the page that
  // is open must not add an entry, just as Next's own push would not.
  if (
    withoutTrailingSlash(url) !== withoutTrailingSlash(pathname + search + hash)
  ) {
    window.history.pushState(window.history.state, "", url);
  }
  router.replace(href, options);
}

function withoutTrailingSlash(url: string): string {
  return url.replace(/\/(?=$|[?#])/, "");
}
