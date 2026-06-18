"use client";

import type { ReactNode } from "react";
import { useScrollSpy } from "@/hooks/use-scroll-spy";
import { EditPageTabs, type EditPageTab } from "./edit-page-tabs";

export interface EditPageShellProps {
  header: ReactNode;
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
      <EditPageTabs
        tabs={tabs}
        activeIndex={activeIndex}
        onSelect={scrollToSection}
      />
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
