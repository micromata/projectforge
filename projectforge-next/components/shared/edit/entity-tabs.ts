import type { EditPageTab } from "@/components/shared/edit-page-tabs";
import type { EntityMetadata } from "@/lib/metadata/types";
import type { ExtraTabDef, SectionDef } from "@/lib/page-def/types";

/** Id of the history tab, so its own page can mark itself as the open one. */
export const HISTORY_TAB_ID = "history";

export interface EntityTabsOptions<M extends EntityMetadata> {
  sections: SectionDef<M>[];
  t: (key: string) => string;
  /**
   * id of the entry, null while it isn't saved yet: a page of its own (its history) has nothing to
   * link to then.
   */
  id: number | null;
  /** Route of the list, e.g. `/cost1` — what the pages beside the form hang off. */
  route: string;
  /**
   * Whether the entity records a change history — then it gets a tab leading to
   * `${route}/${id}/history`, which needs a route of that name to exist. Comes from the generated
   * metadata (`EntityMetadata.historizable`), never from a page declaration: the entity's
   * `@WithHistory` is the only authority, and a second place to say so is a place to drift.
   */
  history?: boolean;
  /**
   * Pages of the entity beside the form and the history, each at `${route}/${id}/${tab.id}` — the same
   * convention the history follows, so a declaration says a key and an id and nothing about routing.
   */
  extraTabs?: ExtraTabDef[];
  /**
   * Whether the bar is rendered on the form itself. Seen from another page of the entity the section
   * tabs are links back to the form, not anchors into a scroll column that isn't there.
   */
  onFormPage: boolean;
}

/**
 * The tab bar of an edit page: one anchor per section, then whatever leads to a page of its own.
 *
 * The anchors must stay in step with the sections array — EditPageShell couples them positionally,
 * which is why both come from the same declaration here.
 */
export function entityTabs<M extends EntityMetadata>({
  sections,
  t,
  id,
  route,
  history,
  extraTabs,
  onFormPage,
}: EntityTabsOptions<M>): EditPageTab[] {
  const formPage = id != null && !onFormPage ? `${route}/${id}` : undefined;
  const anchors = sections.map((section) => ({
    id: section.id,
    label: t(section.tabTitleKey ?? section.titleKey),
    // The section goes into the hash: seen from another page of the entity this is a navigation, and
    // without it the form would mount at its first section no matter which tab was clicked.
    href: formPage && `${formPage}#${section.id}`,
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
            href: `${route}/${id}/history`,
          },
        ]
      : []),
    ...(extraTabs ?? []).map((tab) => ({
      id: tab.id,
      label: t(tab.labelKey),
      href: `${route}/${id}/${tab.id}`,
    })),
  ];
}
