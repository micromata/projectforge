import { create } from "zustand";
import { persist } from "zustand/middleware";

/** Where the panel's top-left corner sits, in viewport pixels; null until it was first moved. */
export interface PanelPosition {
  x: number;
  y: number;
}

interface CostSearchPanelState {
  open: boolean;
  collapsed: boolean;
  position: PanelPosition | null;
  toggle: () => void;
  /** Opens it expanded, whatever it was before ("Show on every page" of the cost search page). */
  show: () => void;
  close: () => void;
  setCollapsed: (collapsed: boolean) => void;
  setPosition: (position: PanelPosition) => void;
}

/**
 * The floating cost search panel (CostSearchPanelHost): whether it is shown, collapsed to its title bar,
 * and where. Remembered in the local storage, so it stays where a finance user put it, across pages and
 * reloads — per browser, as the position only makes sense for the screen it was chosen on.
 *
 * `skipHydration`: the static export renders the closed panel, so reading the storage while the store is
 * created would make the first client render differ from that HTML. The host rehydrates in an effect.
 */
export const useCostSearchPanelStore = create<CostSearchPanelState>()(
  persist(
    (set) => ({
      open: false,
      collapsed: false,
      position: null,
      toggle: () => set((s) => ({ open: !s.open, collapsed: false })),
      show: () => set({ open: true, collapsed: false }),
      close: () => set({ open: false }),
      setCollapsed: (collapsed) => set({ collapsed }),
      setPosition: (position) => set({ position }),
    }),
    {
      name: "pf.costSearchPanel",
      skipHydration: true,
      partialize: ({ open, collapsed, position }) => ({
        open,
        collapsed,
        position,
      }),
    }
  )
);

/** The cost search page's route (the menu entry's target, `next/costSearch`). */
export const COST_SEARCH_ROUTE = "/costSearch";

/**
 * Whether following a menu entry to `href` opens the floating cost search instead (MenuLink,
 * useNavigateMenuUrl): finance users want it beside what they are working on, not instead of it. Not on the
 * page itself, where the entry just stays there as any menu entry of the current page does.
 */
export function opensCostSearchPanel(href: string): boolean {
  return (
    href === COST_SEARCH_ROUTE &&
    !window.location.pathname.endsWith(COST_SEARCH_ROUTE)
  );
}
