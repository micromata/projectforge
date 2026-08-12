"use client";

import { useEffect, type ReactNode } from "react";
import { useScrollSpy } from "@/hooks/use-scroll-spy";
import { EditPageTabs, type EditPageTab } from "./edit-page-tabs";

export interface EditPageShellProps {
  header: ReactNode;
  /**
   * The page's tabs. Anchor tabs (no `href`) come first and are positionally coupled to `sections`;
   * tabs with an `href` lead to their own page and are appended after them.
   */
  tabs: EditPageTab[];
  sections: ReactNode[];
  actions?: ReactNode;
  /**
   * Sticky bar between the tab strip and the scrollable content — stays visible while the user
   * scrolls through the sections (e.g. the order's number/status/sums strip).
   */
  banner?: ReactNode;
}

export function EditPageShell({
  header,
  tabs,
  sections,
  actions,
  banner,
}: EditPageShellProps) {
  const { scrollProps, sectionRef, activeIndex, scrollToSection } =
    useScrollSpy(sections.length);
  useSectionFromHash(tabs, scrollToSection);

  return (
    <div className="flex min-w-0 flex-1 flex-col overflow-hidden">
      <div className="shrink-0">{header}</div>
      {/* A single tab is no choice: an entity with one section and no page of its own (a cost unit
          has neither history nor attachments) would show a bar with one item that does nothing. */}
      {tabs.length > 1 && (
        <EditPageTabs
          tabs={tabs}
          activeIndex={activeIndex}
          onSelect={scrollToSection}
        />
      )}
      {banner && <div className="shrink-0">{banner}</div>}
      <div
        {...scrollProps}
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
    </div>
  );
}

/**
 * Opens the section the URL's hash names, once, on entering the page.
 *
 * The hash is what a section tab on another page of the entity (its history, its forecast) links back
 * to — those tabs are links, not anchors into a column that isn't there, so without this the form
 * would always start at its first section. See `entityTabs`.
 */
function useSectionFromHash(
  tabs: EditPageTab[],
  scrollToSection: (index: number) => void
) {
  // The anchor tabs are the sections in order. Joined to a string so the effect isn't rerun for every
  // fresh array a render produces.
  const anchorIds = tabs
    .filter((tab) => !tab.href)
    .map((tab) => tab.id)
    .join(",");
  useEffect(() => {
    const index = anchorIds.split(",").indexOf(window.location.hash.slice(1));
    // Not `>= 0`: the first section is where the page opens anyway, and scrolling to it would undo a
    // scroll restored by the browser on a reload.
    if (index > 0) scrollToSection(index);
  }, [anchorIds, scrollToSection]);
}
