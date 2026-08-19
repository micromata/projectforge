import { create } from "zustand";

interface UIState {
  sidebarOpen: boolean;
  toggleSidebar: () => void;
  /**
   * Whether the logo row at the very top of the page is collapsed. Written by whichever scroll
   * container the page scrolls in (see hooks/use-collapse-on-scroll.ts), read by
   * components/shared/logo-row.tsx — the two sit in unrelated trees, since the scrollers are deep
   * inside <main> while the row is a sibling of it.
   */
  logoCollapsed: boolean;
  setLogoCollapsed: (collapsed: boolean) => void;
}

export const useUIStore = create<UIState>((set) => ({
  sidebarOpen: true,
  toggleSidebar: () => set((s) => ({ sidebarOpen: !s.sidebarOpen })),
  logoCollapsed: false,
  // Returns the same state when the value is unchanged: a scroll event arrives for every pixel, and
  // an unguarded set() would notify every subscriber each time. With the guard, subscribers hear
  // about a threshold crossing and nothing else, which is why no throttling is needed on top.
  setLogoCollapsed: (collapsed) =>
    set((s) =>
      s.logoCollapsed === collapsed ? s : { logoCollapsed: collapsed }
    ),
}));
