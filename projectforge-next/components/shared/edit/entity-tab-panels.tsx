import type { ReactNode } from "react";
import { HistorySection } from "@/components/shared/history/history-section";
import type { ExtraTabDef } from "@/lib/page-def/types";
import { HISTORY_TAB_ID } from "./entity-tabs";

/**
 * What the tabs beside the form show, by tab id — the counterpart of [entityTabs], which builds the
 * strip from the same declaration.
 *
 * Elements, not components: EditPageShell renders only the open one, which is what keeps the history
 * from being fetched while the form is being filled in.
 *
 * Empty for an entry that isn't stored yet: there is neither a history nor an analysis of it.
 */
export function entityTabPanels({
  entity,
  id,
  history,
  extraTabs,
}: {
  entity: string;
  id: number | null;
  history?: boolean;
  extraTabs?: ExtraTabDef[];
}): Record<string, ReactNode> {
  if (id == null) return {};
  const panels: Record<string, ReactNode> = {};
  if (history) {
    panels[HISTORY_TAB_ID] = <HistorySection entity={entity} entityId={id} />;
  }
  for (const tab of extraTabs ?? []) {
    panels[tab.id] = <tab.component id={id} />;
  }
  return panels;
}
