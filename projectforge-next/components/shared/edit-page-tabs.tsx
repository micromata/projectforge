"use client";

import { cn } from "@/lib/utils";

export interface EditPageTab {
  id: string;
  label: string;
}

export interface EditPageTabsProps {
  tabs: EditPageTab[];
  activeIndex: number;
  onSelect: (index: number) => void;
}

export function EditPageTabs({ tabs, activeIndex, onSelect }: EditPageTabsProps) {
  return (
    <div
      role="tablist"
      className="flex shrink-0 items-end border-b-[1.5px] border-border bg-background px-6"
    >
      {tabs.map((tab, i) => {
        const selected = i === activeIndex;
        return (
          <button
            key={tab.id}
            type="button"
            role="tab"
            aria-selected={selected}
            onClick={() => onSelect(i)}
            className={cn(
              "-mb-px whitespace-nowrap border-b-2 px-4 py-2.5 text-sm transition-colors",
              selected
                ? "border-primary font-bold text-primary"
                : "border-transparent text-foreground/70 hover:text-primary"
            )}
          >
            {tab.label}
          </button>
        );
      })}
    </div>
  );
}
