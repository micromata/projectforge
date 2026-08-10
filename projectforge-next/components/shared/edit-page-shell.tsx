"use client";

import type { ReactNode } from "react";
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
}

export function EditPageShell({
  header,
  tabs,
  sections,
  actions,
}: EditPageShellProps) {
  const { scrollRef, sectionRef, activeIndex, scrollToSection, onScroll } =
    useScrollSpy(sections.length);

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
      <div
        ref={scrollRef}
        onScroll={onScroll}
        className="flex-1 overflow-y-auto bg-muted/30 px-6 pb-6"
      >
        {sections.map((section, i) => (
          <div key={tabs[i]?.id ?? i} ref={sectionRef(i)} className="pt-4">
            {section}
          </div>
        ))}
      </div>
      {actions && <div className="shrink-0">{actions}</div>}
    </div>
  );
}
