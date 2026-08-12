"use client";

import { DynamicLayoutProvider } from "./dynamic-context";
import { DynamicRenderer } from "./dynamic-renderer";
import { DynamicActionGroup } from "./dynamic-action-group";
import { LegacyPageLink } from "@/components/shared/legacy-page-link";
import type { DynamicPageResponse } from "@/lib/rs/types";
import type { ReactNode } from "react";

interface DynamicPageProps {
  response: DynamicPageResponse;
  category: string;
  queryKey: readonly unknown[];
  /** Rendered below the layout, above the action buttons (list pages add their result info here). */
  children?: ReactNode;
}

/**
 * Frame shared by all server-laid-out pages: title, layout, action buttons and the layout the
 * backend wants below them (`UILayout.layoutBelowActions`, used for history tables).
 */
export function DynamicPage({
  response,
  category,
  queryKey,
  children,
}: DynamicPageProps) {
  return (
    <DynamicLayoutProvider
      response={response}
      category={category}
      queryKey={queryKey}
    >
      <div className="flex flex-1 flex-col overflow-hidden">
        {/* The link is an action of the page, not part of its title, so it is rendered even without one. */}
        {(response.ui.title || response.ui.legacyUrl) && (
          <div className="flex items-center gap-2 px-6 pt-4 pb-2">
            {response.ui.title && (
              <h1 className="text-xl font-semibold">{response.ui.title}</h1>
            )}
            <div className="flex-1" />
            <LegacyPageLink url={response.ui.legacyUrl} />
          </div>
        )}
        <div className="flex-1 overflow-auto px-6 pb-6">
          <div className="flex flex-col gap-4">
            <DynamicRenderer content={response.ui.layout} />
          </div>
          {children}
        </div>
        <DynamicActionGroup />
        {response.ui.layoutBelowActions && (
          <div className="px-6 pb-6">
            <DynamicRenderer content={response.ui.layoutBelowActions} />
          </div>
        )}
      </div>
    </DynamicLayoutProvider>
  );
}
