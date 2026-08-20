"use client";

import Link from "next/link";
import { usePathname, useSearchParams } from "next/navigation";
import { cn } from "@/lib/utils";

/** Query parameter naming the open tab beside the form — see EditPageShell. */
export const TAB_PARAM = "tab";

export interface EditPageTab {
  id: string;
  label: string;
  /**
   * Tab id in the URL (`?tab=history`), for a tab that replaces the form: it is a link, so it is
   * shareable and the browser's back button works. A tab without one is an anchor into the current
   * page's scroll column (see EditPageShell).
   *
   * Deliberately not a route of its own: a route change unmounts the form, and everything the user
   * had entered goes with it.
   */
  tab?: string;
}

export interface EditPageTabsProps {
  tabs: EditPageTab[];
  /** Selected anchor tab, ignored while `activeId` names the selected one. */
  activeIndex?: number;
  /** Id of the selected tab, set while one of the tabs beside the form is open. */
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
  // The params are kept, so a tab link doesn't drop what else the URL carries (`?clone=1` on an
  // added entry). The pathname is spelled out rather than left to a bare `?tab=…`: the same page is
  // reached under `/next/invoice/1` in production and `/invoice/1` in development, and a relative
  // href would have to be resolved against whichever it is.
  const params = useSearchParams();
  const pathname = usePathname();

  return (
    <div
      role="tablist"
      className="flex shrink-0 items-end border-b-[1.5px] border-border bg-background px-6"
    >
      {tabs.map((tab, i) => {
        // activeId wins: while a tab beside the form is open, the form's scroll position says nothing
        // about which tab that is.
        const selected = activeId ? tab.id === activeId : i === activeIndex;
        return tab.tab ? (
          <Link
            key={tab.id}
            href={`${pathname}?${tabQuery(params, tab.tab)}`}
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

function tabQuery(params: URLSearchParams, tab: string): string {
  const next = new URLSearchParams(params);
  next.set(TAB_PARAM, tab);
  return next.toString();
}
