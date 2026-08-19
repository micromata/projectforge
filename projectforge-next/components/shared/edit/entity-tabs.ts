import type { EditPageTab } from "@/components/shared/edit-page-tabs";
import { leafKeyOf } from "@/lib/leaf-key";
import type { EntityMetadata } from "@/lib/metadata/types";
import type { ExtraTabDef, SectionDef } from "@/lib/page-def/types";

/** Id of the history tab, so its own page can mark itself as the open one. */
export const HISTORY_TAB_ID = "history";

export interface EntityTabsOptions<M extends EntityMetadata> {
  sections: SectionDef<M>[];
  /** The translator, `has` included: a section's key may be one that needs [leafKeyOf]. */
  t: ((key: string) => string) & { has: (key: string) => boolean };
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
  /**
   * Query string (without the `?`) every link to a page of the entity carries along — `returnTo=…`,
   * so a detour through the history does not forget where the user came from (see useEditReturn).
   */
  query?: string;
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
  query,
}: EntityTabsOptions<M>): EditPageTab[] {
  const suffix = query ? `?${query}` : "";
  const formPage = id != null && !onFormPage ? `${route}/${id}` : undefined;
  const anchors = sections.map((section) => ({
    id: section.id,
    // Through leafKeyOf like every other backend key a renderer hands to `t()`: a section named after
    // its entity carries a key that is a text *and* a namespace (`fibu.rechnung`), and the bare one
    // resolves to an object. The declaration must not have to know which of its keys collide.
    label: t(leafKeyOf(section.tabTitleKey ?? section.titleKey, t.has)),
    // The section goes into the hash: seen from another page of the entity this is a navigation, and
    // without it the form would mount at its first section no matter which tab was clicked.
    href: formPage && `${formPage}${suffix}#${section.id}`,
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
            href: `${route}/${id}/history${suffix}`,
          },
        ]
      : []),
    ...(extraTabs ?? []).map((tab) => ({
      id: tab.id,
      label: t(leafKeyOf(tab.labelKey, t.has)),
      href: `${route}/${id}/${tab.id}${suffix}`,
    })),
  ];
}
