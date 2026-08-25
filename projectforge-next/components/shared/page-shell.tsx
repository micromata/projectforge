"use client";

import { useEffect, type ReactNode } from "react";
import { usePathname } from "next/navigation";
import { BrandStripe } from "@/components/shared/brand-stripe";
import { LogoRow } from "@/components/shared/logo-row";
import { SystemAlertBanner } from "@/components/shared/system-alert-banner";
import { TopNavigation } from "@/components/shared/top-navigation";
import { useCollapseOnScroll } from "@/hooks/use-collapse-on-scroll";
import { useUIStore } from "@/store/ui-store";

interface PageShellProps {
  children: ReactNode;
}

export function PageShell({ children }: PageShellProps) {
  // <main> is the scroll column of every page that has no inner scroller of its own (the start page,
  // the task tree, a mass update); the pages that do opt in at their own container instead.
  const collapse = useCollapseOnScroll();
  const setLogoCollapsed = useUIStore((s) => s.setLogoCollapsed);
  const pathname = usePathname();

  // Going from one entry to the next (/book/1 -> /book/2) reconciles the same elements, so no scroll
  // column unmounts and the hook's own cleanup does not run - but the new page starts at the top, so
  // the row belongs back. Where the browser restores a scroll position the column says so right after.
  useEffect(() => {
    setLogoCollapsed(false);
  }, [pathname, setLogoCollapsed]);

  return (
    // `relative`, so this box is the containing block of every absolutely positioned descendant that
    // has no positioned parent of its own — Tailwind's `sr-only` is one (the file inputs behind the
    // attachment buttons). Without it their containing block is <body>: they are laid out at their
    // static position deep inside a scroll column, escape that column's clipping (clipping follows the
    // containing block chain, not the DOM parent chain) and make the *document* as tall as the whole
    // form. The document then scrolls, and at the end of a column that scroll chains into it, lifting
    // the app - the action bar included - out of the viewport and leaving white below it. With the
    // containing block here, `overflow-hidden` clips them and the page itself never scrolls.
    // `h-dvh`, not `h-screen`: on iOS Safari `100vh` is the *large* viewport (the area behind the
    // collapsible toolbars), so the box is taller than what's on screen and the pinned action bar of
    // an edit page falls below the fold — reachable only by scrolling the outer <main>, never the
    // inner form column. `100dvh` tracks the visible viewport, so the bar stays anchored at the true
    // bottom and <main> keeps no extra overflow to scroll.
    <div className="relative flex h-dvh flex-col overflow-hidden">
      <LogoRow />
      <BrandStripe />
      <TopNavigation />
      {/* Here and not in the authenticated layout: the announcement belongs under the navigation of
          every page, as in Wicket, and this shell is what every page of this app is built from. */}
      <SystemAlertBanner />
      <main
        className="flex flex-1 flex-col overflow-auto"
        onScroll={collapse.onScroll}
      >
        {children}
      </main>
    </div>
  );
}
