"use client";

import Link from "next/link";
import { cn } from "@/lib/utils";

export interface EditPageTab {
  id: string;
  label: string;
  /**
   * Route this tab leads to. A tab with an href is a real tab: it navigates to its own page, so
   * its content is only mounted (and only fetched) while it is open. A tab without one is an
   * anchor into the current page's scroll column (see EditPageShell).
   */
  href?: string;
}

export interface EditPageTabsProps {
  tabs: EditPageTab[];
  /** Selected anchor tab, ignored when `activeId` names the selected one. */
  activeIndex?: number;
  /** Id of the selected tab. Set it on a page that *is* one of the tabs, e.g. the history page. */
  activeId?: string;
  onSelect?: (index: number) => void;
}

const TAB_CLASS =
  "-mb-px whitespace-nowrap border-b-2 px-4 py-2.5 text-sm transition-colors";

function tabClass(selected: boolean): string {
  return cn(
    TAB_CLASS,
    selected
      ? "border-primary font-bold text-primary"
      : "border-transparent text-foreground/70 hover:text-primary"
  );
}

export function EditPageTabs({
  tabs,
  activeIndex,
  activeId,
  onSelect,
}: EditPageTabsProps) {
  return (
    <div
      role="tablist"
      className="flex shrink-0 items-end border-b-[1.5px] border-border bg-background px-6"
    >
      {tabs.map((tab, i) => {
        // activeId wins: on a tab's own page the scroll position says nothing about which tab is open.
        const selected = activeId ? tab.id === activeId : i === activeIndex;
        return tab.href ? (
          <Link
            key={tab.id}
            href={tab.href}
            role="tab"
            aria-selected={selected}
            className={tabClass(selected)}
          >
            {tab.label}
          </Link>
        ) : (
          <button
            key={tab.id}
            type="button"
            role="tab"
            aria-selected={selected}
            onClick={() => onSelect?.(i)}
            className={tabClass(selected)}
          >
            {tab.label}
          </button>
        );
      })}
    </div>
  );
}
