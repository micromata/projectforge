"use client";

import { Activity, useEffect, useRef, type ReactNode } from "react";
import { useSearchParams } from "next/navigation";
import { useCollapseOnScroll } from "@/hooks/use-collapse-on-scroll";
import { useScrollSpy } from "@/hooks/use-scroll-spy";
import { EditPageTabs, TAB_PARAM, type EditPageTab } from "./edit-page-tabs";

export interface EditPageShellProps {
  header: ReactNode;
  /**
   * The page's tabs. Anchor tabs (no `tab`) come first and are positionally coupled to `sections`;
   * the tabs that replace the form are appended after them.
   */
  tabs: EditPageTab[];
  sections: ReactNode[];
  actions?: ReactNode;
  /**
   * Sticky bar between the tab strip and the scrollable content — stays visible while the user
   * scrolls through the sections (e.g. the order's number/status/sums strip).
   */
  banner?: ReactNode;
  /**
   * What a tab beside the form shows, by tab id (`history`, `forecast`). Only the open one is
   * rendered, so its content is fetched when it is looked at and not before — building the history
   * of a long-lived entity is expensive on the server.
   */
  tabPanels?: Record<string, ReactNode>;
}

export function EditPageShell({
  header,
  tabs,
  sections,
  actions,
  banner,
  tabPanels,
}: EditPageShellProps) {
  const { scrollProps, sectionRef, activeIndex, scrollToSection } =
    useScrollSpy(sections.length);
  const collapse = useCollapseOnScroll();
  // Which tab beside the form is open, unset while the form is. In the URL rather than in state, so
  // the tab is shareable and the browser's back button returns to the form — and a search parameter
  // rather than a route of its own, because a route change would unmount the form and take
  // everything the user had entered with it ("Search params do not trigger remounts").
  const params = useSearchParams();
  const requested = params.get(TAB_PARAM);
  const activeTab = requested && tabPanels?.[requested] ? requested : null;
  // A section tab clicked while a side tab is open is how a user comes back to the form, so it closes
  // that tab first and scrolls once the form is on screen again — a hidden column cannot be scrolled,
  // it has no layout.
  // A ref, not state: nothing renders differently for it, it only says what the effect below still
  // owes — and a render of its own is exactly what must not happen between closing the tab and
  // scrolling.
  const pendingSection = useRef<number | null>(null);

  useEffect(() => {
    if (activeTab || pendingSection.current == null) return;
    scrollToSection(pendingSection.current);
    pendingSection.current = null;
  }, [activeTab, scrollToSection]);

  function selectSection(index: number): void {
    if (!activeTab) {
      scrollToSection(index);
      return;
    }
    pendingSection.current = index;
    const next = new URLSearchParams(params);
    next.delete(TAB_PARAM);
    const query = next.toString();
    // The native History API, not `router.push`: this changes nothing but a search parameter, which is
    // the case Next names it for ("pushState and replaceState calls integrate into the Next.js
    // Router, allowing you to sync with usePathname and useSearchParams").
    //
    // And `router.push` does not merely cost more here, it does not arrive: on a deep link of the
    // static export — `/next/task/42?tab=history`, which is where the old history url redirects to —
    // the push fetches the route's RSC payload and then puts the old url back, so the form never
    // reappears and the tab bar is stuck on the history. The route was prerendered under a
    // placeholder id (see useRouteParams), so there is no entry for this url for the push to commit.
    window.history.pushState(
      null,
      "",
      query ? `?${query}` : window.location.pathname
    );
  }

  return (
    <div className="flex min-w-0 flex-1 flex-col overflow-hidden">
      <div className="shrink-0">{header}</div>
      {/* A single tab is no choice: an entity with one section and no tab of its own (a cost unit has
          neither history nor attachments) would show a bar with one item that does nothing. */}
      {tabs.length > 1 && (
        <EditPageTabs
          tabs={tabs}
          activeIndex={activeIndex}
          activeId={activeTab ?? undefined}
          onSelect={selectSection}
        />
      )}
      {/* Hidden, not unmounted: React keeps the tree alive with its state — form values, expanded
          rows, scroll position — and the whole point of the tabs living in one route is that going
          to the history and back doesn't throw away what was being filled in. Note that effects do
          re-run on the way back to visible (see the reset guard in useEntityEditForm). */}
      <Activity mode={activeTab ? "hidden" : "visible"} name="edit-form">
        {banner && <div className="shrink-0">{banner}</div>}
        <div
          {...scrollProps}
          // Two listeners on the one column: the spy derives the active section, the collapse drives
          // the logo row. React attaches `scroll` per element rather than delegating it, so a second
          // handler has to be composed here - and the spread has to come first, or it would drop this
          // one.
          onScroll={(event) => {
            scrollProps.onScroll();
            collapse.onScroll(event);
          }}
          className="flex-1 overflow-y-auto bg-muted/30 px-6 pb-6"
        >
          {sections.map((section, i) => (
            <div
              key={tabs[i]?.id ?? i}
              ref={sectionRef(i)}
              // `data-active` is what the section's card and heading read to highlight themselves —
              // the later sections can never reach the top of the column, so the card is the only
              // place that says unambiguously which one the tab bar means.
              data-active={i === activeIndex}
              className="group/section pt-4"
            >
              {section}
            </div>
          ))}
        </div>
        {actions && <div className="shrink-0">{actions}</div>}
      </Activity>
      {activeTab && (
        <div
          className="flex-1 overflow-y-auto bg-muted/30 px-6 pt-4 pb-6"
          onScroll={collapse.onScroll}
        >
          {tabPanels?.[activeTab]}
        </div>
      )}
    </div>
  );
}
