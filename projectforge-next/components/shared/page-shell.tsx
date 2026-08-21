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
    <div className="flex h-screen flex-col overflow-hidden">
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
