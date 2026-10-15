"use client";

import { useEffect } from "react";
import { APP_TITLE, windowTitle } from "@/lib/window-title";

/**
 * Names the browser window after the page, see windowTitle.
 *
 * On the client and not via Next's `metadata`: under `output: "export"` the pages are prerendered once
 * in the default locale, while their titles are translated (or come from the server) only in the
 * browser. Called by the shared headings (PageTitleRow, EntityEditHeader, AuthCard, …) rather than by
 * each page, so a page gets its title by having a heading. Unmounting falls back to the bare name; the
 * next page's own effect runs after that cleanup, so a stale title never survives a navigation.
 */
export function useDocumentTitle(title?: string | null) {
  useEffect(() => {
    document.title = windowTitle(title);
    return () => {
      document.title = APP_TITLE;
    };
  }, [title]);
}
