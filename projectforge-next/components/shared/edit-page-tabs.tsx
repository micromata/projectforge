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
  /**
   * Opens a tab beside the form by its id. When given, those tabs are buttons driven by local state
   * instead of `?tab=` links — what a modal needs, having no shareable URL (see EntityEditDialogShell).
   */
  onSelectTab?: (tab: string) => void;
}

// A segmented control (mirrors the shadcn Tabs `default` variant): the whole bar is one rounded,
// muted track, the selected tab a raised card inside it. Reads as one group even when it wraps to a
// second row — an underline bar loses its shared baseline the moment it does (see EditPageTabs).
const TAB_CLASS =
  "rounded-md px-3 py-1.5 text-sm font-medium whitespace-nowrap transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring";

function tabClass(selected: boolean): string {
  return cn(
    TAB_CLASS,
    selected
      ? "bg-background font-semibold text-foreground shadow-sm"
      : "text-foreground/60 hover:text-foreground"
  );
}

export function EditPageTabs({
  tabs,
  activeIndex,
  activeId,
  onSelect,
  onSelectTab,
}: EditPageTabsProps) {
  // The params are kept, so a tab link doesn't drop what else the URL carries (`?clone=1` on an
  // added entry). The pathname is spelled out rather than left to a bare `?tab=…`: the same page is
  // reached under `/next/invoice/1` in production and `/invoice/1` in development, and a relative
  // href would have to be resolved against whichever it is.
  const params = useSearchParams();
  const pathname = usePathname();

  return (
    <div className="shrink-0 border-b-[1.5px] border-border bg-background px-6 py-2">
      <div
        role="tablist"
        className="flex w-fit max-w-full flex-wrap items-center gap-1 rounded-lg bg-muted p-1"
      >
        {tabs.map((tab, i) => {
          // activeId wins: while a tab beside the form is open, the form's scroll position says nothing
          // about which tab that is.
          const selected = activeId ? tab.id === activeId : i === activeIndex;
          if (tab.tab) {
            // Controlled: a modal has no shareable URL, so its side-tabs are buttons over local state.
            const sideTab = tab.tab;
            return onSelectTab ? (
              <button
                key={tab.id}
                type="button"
                role="tab"
                aria-selected={selected}
                onClick={() => onSelectTab(sideTab)}
                className={tabClass(selected)}
              >
                {tab.label}
              </button>
            ) : (
              <Link
                key={tab.id}
                href={`${pathname}?${tabQuery(params, sideTab)}`}
                role="tab"
                aria-selected={selected}
                className={tabClass(selected)}
              >
                {tab.label}
              </Link>
            );
          }
          return (
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
    </div>
  );
}

function tabQuery(params: URLSearchParams, tab: string): string {
  const next = new URLSearchParams(params);
  next.set(TAB_PARAM, tab);
  return next.toString();
}
