"use client";

import { useCallback } from "react";
import { useRouter } from "next/navigation";
import { resolveMenuUrl, toAbsoluteUrl } from "@/lib/menu-url";
import { confirmLeaveUnsavedChanges } from "@/hooks/use-unsaved-changes-warning";

/**
 * Navigates to a backend url (a menu url or a search hit's `viewUrl`), the way the quick access search does.
 *
 * The legacy React app and Wicket are served by Spring, not by this app: a client-side route would land on
 * Next's own 404, so `next/…` goes through the router while `react/…`/`wa/…` do a full page load (which
 * `beforeunload` guards on its own). A `router.push` is not a link and nothing else would stop it, so an
 * internal navigation asks the app's own unsaved-changes dialog first.
 *
 * Cross-cutting (used by the menu results, the magnifier's live data hits and the search page), so it lives in
 * hooks/, not the search feature.
 */
export function useNavigateMenuUrl() {
  const router = useRouter();
  return useCallback(
    (url: string, beforeNavigate?: () => void) => {
      const target = resolveMenuUrl(url);
      if (target.kind === "external") {
        beforeNavigate?.();
        window.location.assign(toAbsoluteUrl(target));
        return;
      }
      void confirmLeaveUnsavedChanges().then((leave) => {
        if (!leave) return;
        beforeNavigate?.();
        router.push(target.href);
      });
    },
    [router]
  );
}
