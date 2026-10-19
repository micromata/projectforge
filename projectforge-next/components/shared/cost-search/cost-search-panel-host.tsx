"use client";

import { useEffect } from "react";
import { usePathname } from "next/navigation";
import { useCostSearchPanelStore } from "@/store/cost-search-panel-store";
import { CostSearchPanel } from "./cost-search-panel";
import { useCostSearchAccess } from "./use-cost-search-access";
import { useCostSearchShortcut } from "./use-cost-search-shortcut";
import {
  useDockCostSearch,
  useTrackInAppNavigation,
} from "./use-dock-cost-search";

/**
 * Mounts the floating cost search (CostSearchPanel) on every page, while the user has opened it (top bar
 * button or `ALT-K`) and may use it. Not on the cost search's own page and pop-out window: there it
 * would show the same twice. On the page, `ALT-K` docks the page into the panel instead (useDockCostSearch);
 * in the pop-out window it does nothing, the window is the cost search already.
 */
export function CostSearchPanelHost() {
  const allowed = useCostSearchAccess();
  const pathname = usePathname();
  const open = useCostSearchPanelStore((s) => s.open);
  const toggle = useCostSearchPanelStore((s) => s.toggle);
  useTrackInAppNavigation();
  const dock = useDockCostSearch();
  const onOwnPage = pathname.startsWith("/costSearch");
  const inPopout = pathname.startsWith("/costSearch/popout");

  // The store skips hydration (see useCostSearchPanelStore): read the stored state after the first render.
  useEffect(() => {
    void useCostSearchPanelStore.persist.rehydrate();
  }, []);

  useCostSearchShortcut(onOwnPage ? dock : toggle, allowed && !inPopout);

  if (!allowed || !open || onOwnPage) return null;
  return <CostSearchPanel />;
}
