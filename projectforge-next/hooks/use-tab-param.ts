"use client";

import { useSearchParams } from "next/navigation";
import { TAB_PARAM } from "@/components/shared/edit-page-tabs";

/**
 * The open tab of a page's tab bar, kept in the search parameter `?tab=` — so a reload or a shared link
 * opens the same tab and the browser's back button returns to the previous one. A search parameter and
 * not a route per tab, for the reason EditPageShell gives: a route change would remount what the tabs
 * share (the embedded list with its scroll position and selection).
 *
 * `allowed` are the tab ids the page currently offers; a requested tab outside of them (a stale link, a
 * tab the user has no right for) yields `fallback`. `undefined` while the page doesn't know yet — the
 * rights still loading — takes the requested tab as it is, so a deep link to a gated tab isn't lost to a
 * fallback before the answer arrives. The fallback is never written to the url, the plain path is it.
 */
export function useTabParam(
  fallback: string,
  allowed: readonly string[] | undefined
): [string, (tab: string) => void] {
  const params = useSearchParams();
  const requested = params.get(TAB_PARAM);
  const tab =
    requested && (!allowed || allowed.includes(requested))
      ? requested
      : fallback;

  function setTab(next: string): void {
    const query = new URLSearchParams(params);
    if (next === fallback) query.delete(TAB_PARAM);
    else query.set(TAB_PARAM, next);
    const search = query.toString();
    // The native History API, not `router.push` — see EditPageShell.selectSection for why.
    window.history.pushState(
      null,
      "",
      search ? `?${search}` : window.location.pathname
    );
  }

  return [tab, setTab];
}
