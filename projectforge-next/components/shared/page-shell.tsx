"use client";

import { useEffect, type ReactNode } from "react";
import { usePathname } from "next/navigation";
import { BrandStripe } from "@/components/shared/brand-stripe";
import { LogoRow } from "@/components/shared/logo-row";
import { NotificationBanners } from "@/components/shared/notifications/notification-banners";
import { PullToRefreshIndicator } from "@/components/shared/pull-to-refresh-indicator";
import { StatusBar } from "@/components/shared/status-bar";
import { SystemAlertBanner } from "@/components/shared/system-alert-banner";
import { TopNavigation } from "@/components/shared/top-navigation";
import { useCollapseOnScroll } from "@/hooks/use-collapse-on-scroll";
import { isInApp } from "@/lib/app-mode";
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
  // The chat tab of the app has its own native navigation: no logo, menu or status bar there (they would lead
  // the tab away from the chat, which then shows a foreign page when the user returns).
  const bare = isInApp() && pathname.startsWith("/chat");

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
    // containing block here the page itself never scrolls. As this box scrolls on a short viewport
    // (below), a scroll column holding such elements is `relative` itself (see EditPageShell), or they
    // would make this box as tall as the column's content.
    // `h-dvh`, not `h-screen`: on iOS Safari `100vh` is the *large* viewport (the area behind the
    // collapsible toolbars), so the box is taller than what's on screen and the pinned action bar of
    // an edit page falls below the fold — reachable only by scrolling the outer <main>, never the
    // inner form column. `100dvh` tracks the visible viewport, so the bar stays anchored at the true
    // bottom and <main> keeps no extra overflow to scroll.
    // On a short viewport (a phone in landscape) the fixed rows would squeeze <main> to nothing and the
    // content below them would be unreachable. <main> therefore has a floor (24rem, but never more than
    // the viewport) and this box scrolls (`overflow-y-auto`) once rows + floor exceed the height: the rows
    // scroll away, the content and its pinned action bar stay reachable. On a normal screen nothing
    // overflows and nothing changes.
    // On a narrow or low screen (`cramped`) <main> is the size container of the content panels: a
    // chart panel is then exactly as tall as <main> (`100cqh`, see OrderStatisticsPage), so the header
    // above it scrolls away with <main> and the panel gets the whole screen.
    // Printed (`print:`), the page is only its content, as long as it is: no header, navigation or status
    // bar, and no box of the window's height clipping everything below the first page.
    <div className="relative flex h-dvh flex-col overflow-x-hidden overflow-y-auto print:block print:h-auto print:overflow-visible">
      {/* `contents`: on screen as if the wrapper weren't there, the rows stay items of the column. */}
      <div className="contents print:hidden">
        {!bare && (
          <>
            <LogoRow />
            <BrandStripe />
            <TopNavigation />
            {/* Here and not in the authenticated layout: the announcement belongs under the navigation of
            every page, as in Wicket, and this shell is what every page of this app is built from. */}
            <SystemAlertBanner />
            <NotificationBanners />
          </>
        )}
      </div>
      <main
        className="flex min-h-[min(24rem,100dvh)] flex-1 flex-col overflow-auto cramped:[container-type:size] print:block print:overflow-visible"
        onScroll={(event) => {
          // A list on a phone collapses its header by its table's scroll (useListHeaderCollapse). <main>
          // may overflow too there (phone landscape), and the collapse shrinks it back to the table: the
          // clamped scrollTop of 0 would expand the header at once, and the table never stays scrolled.
          if (useUIStore.getState().listHeaderHeight > 0) return;
          collapse.onScroll(event);
        }}
      >
        {children}
      </main>
      {/* Sibling of <main>, not inside it: shrink-0 keeps the strip pinned at the true bottom while
          <main> takes the remaining height, exactly as Wicket's footer sits below the scroll area. */}
      <div className="contents print:hidden">{!bare && <StatusBar />}</div>
      <PullToRefreshIndicator />
    </div>
  );
}
