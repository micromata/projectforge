"use client";

import { createContext, useContext, type ReactNode } from "react";
import { PageShell } from "@/components/shared/page-shell";
import {
  PageTitleRow,
  type PageTitleRowProps,
} from "@/components/shared/page-title-row";

/** The log viewer is shown inside another page (the system dashboard's tab), not as a page of its own. */
const EmbeddedContext = createContext(false);

/** Shows the log viewer's views inside another page: no page shell, no page title (see LogViewerTitle). */
export function LogViewerEmbedded({ children }: { children: ReactNode }) {
  return (
    <EmbeddedContext.Provider value>
      {/* The views keep their own padding: cancel the one of the tab around them. */}
      <div className="-mx-4 flex min-h-0 flex-1 flex-col">{children}</div>
    </EmbeddedContext.Provider>
  );
}

/** The page shell of a log viewer view; nothing of its own when embedded. */
export function LogViewerShell({ children }: { children: ReactNode }) {
  return useContext(EmbeddedContext) ? (
    <>{children}</>
  ) : (
    <PageShell>{children}</PageShell>
  );
}

/**
 * The title row of a log viewer view. Embedded, the page around it has the title: only the controls (the
 * source switch in [center]) and the actions are left, plus the title itself where it names what is shown
 * ([embeddedTitle], e.g. the problem whose occurrences are listed).
 */
export function LogViewerTitle({
  embeddedTitle = false,
  ...props
}: PageTitleRowProps & { embeddedTitle?: boolean }) {
  if (!useContext(EmbeddedContext)) return <PageTitleRow {...props} />;
  return (
    <div className="flex flex-wrap items-center gap-3 px-4 pt-1">
      {embeddedTitle && <h2 className="text-sm font-medium">{props.title}</h2>}
      {props.center}
      <span className="flex-1" />
      {props.children}
    </div>
  );
}
