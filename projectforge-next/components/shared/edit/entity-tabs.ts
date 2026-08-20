import type { EditPageTab } from "@/components/shared/edit-page-tabs";
import { leafKeyOf } from "@/lib/leaf-key";
import type { EntityMetadata } from "@/lib/metadata/types";
import type { ExtraTabDef, SectionDef } from "@/lib/page-def/types";

/** Id of the history tab, both in the tab bar and in the URL (`?tab=history`). */
export const HISTORY_TAB_ID = "history";

export interface EntityTabsOptions<M extends EntityMetadata> {
  sections: SectionDef<M>[];
  /** The translator, `has` included: a section's key may be one that needs [leafKeyOf]. */
  t: ((key: string) => string) & { has: (key: string) => boolean };
  /**
   * id of the entry, null while it isn't saved yet: a tab beside the form (its history) has nothing
   * to show then.
   */
  id: number | null;
  /**
   * Whether the entity records a change history — then it gets a tab of its own. Comes from the
   * generated metadata (`EntityMetadata.historizable`), never from a page declaration: the entity's
   * `@WithHistory` is the only authority, and a second place to say so is a place to drift.
   */
  history?: boolean;
  /** Tabs of the entity beside the form and the history — the order's forecast. */
  extraTabs?: ExtraTabDef[];
}

/**
 * The tab bar of an edit page: one anchor per section, then the tabs that replace the form.
 *
 * The anchors must stay in step with the sections array — EditPageShell couples them positionally,
 * which is why both come from the same declaration here.
 */
export function entityTabs<M extends EntityMetadata>({
  sections,
  t,
  id,
  history,
  extraTabs,
}: EntityTabsOptions<M>): EditPageTab[] {
  const anchors = sections.map((section) => ({
    id: section.id,
    // Through leafKeyOf like every other backend key a renderer hands to `t()`: a section named after
    // its entity carries a key that is a text *and* a namespace (`fibu.rechnung`), and the bare one
    // resolves to an object. The declaration must not have to know which of its keys collide.
    label: t(leafKeyOf(section.tabTitleKey ?? section.titleKey, t.has)),
  }));
  if (id == null) return anchors;
  return [
    ...anchors,
    ...(history
      ? [
          {
            id: HISTORY_TAB_ID,
            // The backend's own name for it, the same heading the section carries.
            label: t("label.historyOfChanges"),
            tab: HISTORY_TAB_ID,
          },
        ]
      : []),
    ...(extraTabs ?? []).map((tab) => ({
      id: tab.id,
      label: t(leafKeyOf(tab.labelKey, t.has)),
      tab: tab.id,
    })),
  ];
}
