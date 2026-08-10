import type { EditPageTab } from "@/components/shared/edit-page-tabs";
import type { EntityMetadata } from "@/lib/metadata/types";
import type { SectionDef } from "@/lib/page-def/types";

/**
 * The tab bar of an edit page: one anchor per section, then whatever leads to a page of its own.
 *
 * The anchors must stay in step with the sections array — EditPageShell couples them positionally,
 * which is why both come from the same declaration here.
 *
 * @param id null for an entry that isn't saved yet: a page of its own (its history) has nothing to
 *   link to.
 */
export function entityTabs<M extends EntityMetadata>(
  sections: SectionDef<M>[],
  t: (key: string) => string,
  id: number | null,
  extraTabs?: (id: number) => EditPageTab[]
): EditPageTab[] {
  const anchors = sections.map((section) => ({
    id: section.id,
    label: t(section.titleKey),
  }));
  if (id == null || !extraTabs) return anchors;
  return [...anchors, ...extraTabs(id)];
}
