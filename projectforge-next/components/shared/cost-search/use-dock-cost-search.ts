"use client";

import { useCallback, useEffect } from "react";
import { usePathname, useRouter } from "next/navigation";
import { useCostSearchPanelStore } from "@/store/cost-search-panel-store";

/** The route last seen, and whether it changed since this app was loaded (see useTrackInAppNavigation). */
let lastPathname: string | null = null;
let navigatedInApp = false;

/**
 * Counts the client-side route changes, so that docking knows whether the page before is one of this app.
 * Called once, by the always mounted CostSearchPanelHost. Compares routes rather than counting runs: the
 * development mode runs every effect twice.
 */
export function useTrackInAppNavigation() {
  const pathname = usePathname();
  useEffect(() => {
    if (lastPathname !== null && lastPathname !== pathname)
      navigatedInApp = true;
    lastPathname = pathname;
  }, [pathname]);
}

/**
 * "Docks" the cost search page into the floating panel: opens the panel and leaves the page — back to the
 * page of this app the user came from, so the panel floats over it. Without one (opened from the legacy
 * React menu, a link or a reload) to the start page instead: going back would leave this app, and only
 * its pages show the panel. What the page's button and `ALT-K` on the page do.
 */
export function useDockCostSearch(): () => void {
  const router = useRouter();
  const show = useCostSearchPanelStore((s) => s.show);
  return useCallback(() => {
    show();
    if (navigatedInApp) router.back();
    else router.push("/");
  }, [router, show]);
}
