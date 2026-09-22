"use client";

import { Activity, useEffect, useRef, useState } from "react";
import { DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { useScrollSpy } from "@/hooks/use-scroll-spy";
import { EditPageTabs } from "../edit-page-tabs";
import type { EditRegions } from "./entity-edit-body";

/**
 * The modal counterpart of [EditPageShell]: the same tab strip, scrolled section column and sticky
 * action bar, but with the open side-tab held in local state instead of the URL — a modal has no
 * shareable link of its own, and a route change would unmount the form (see EditPageShell). Rendered
 * inside a [DialogContent] by [EntityEditModal]; the regions are computed once by [EntityEditBody].
 *
 * `initialTab` seeds that state once: a modal opened *by* a url (the calendar's `/calendar/timesheet/5
 * ?tab=history`, see calendar/[...edit]) starts on the named tab, ignored unless the entity actually
 * has it — the form is the fallback.
 */
export function EntityEditDialogShell({
  regions,
  initialTab,
}: {
  regions: EditRegions;
  initialTab?: string;
}) {
  const { scrollProps, sectionRef, activeIndex, scrollToSection } =
    useScrollSpy(regions.sections.length);
  // Which tab beside the form is open, null while the form is. Local state, not `?tab=`.
  const [activeTab, setActiveTab] = useState<string | null>(
    initialTab && regions.tabPanels[initialTab] ? initialTab : null
  );
  // A section tab clicked while a side tab is open first closes that tab, then scrolls once the form
  // is on screen again — a hidden column has no layout to scroll (same dance as EditPageShell).
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
    setActiveTab(null);
  }

  function selectTab(tab: string): void {
    setActiveTab(regions.tabPanels[tab] ? tab : null);
  }

  return (
    <>
      <DialogHeader className="shrink-0 border-b px-6 py-4 pr-12">
        <DialogTitle>{regions.title}</DialogTitle>
      </DialogHeader>
      {regions.tabs.length > 1 && (
        <EditPageTabs
          tabs={regions.tabs}
          activeIndex={activeIndex}
          activeId={activeTab ?? undefined}
          onSelect={selectSection}
          onSelectTab={selectTab}
        />
      )}
      {/* Hidden, not unmounted, so going to the history and back keeps what was being filled in. */}
      <Activity mode={activeTab ? "hidden" : "visible"} name="edit-form">
        {regions.banner && <div className="shrink-0">{regions.banner}</div>}
        <div
          {...scrollProps}
          className="min-h-0 flex-1 overflow-y-auto bg-muted/30 px-6 pb-6"
        >
          {regions.sections.map((section, i) => (
            <div
              key={regions.tabs[i]?.id ?? i}
              ref={sectionRef(i)}
              data-active={i === activeIndex}
              className="group/section pt-4"
            >
              {typeof section === "function"
                ? section(i === activeIndex)
                : section}
            </div>
          ))}
          {regions.belowSections && (
            <div className="pt-4">{regions.belowSections}</div>
          )}
        </div>
        <div className="shrink-0">{regions.actions}</div>
      </Activity>
      {activeTab && (
        <div className="min-h-0 flex-1 overflow-y-auto bg-muted/30 px-6 pt-4 pb-6">
          {regions.tabPanels[activeTab]}
        </div>
      )}
    </>
  );
}
