"use client";

import { useRouter } from "next/navigation";
import { EditPageShell } from "@/components/shared/edit-page-shell";
import type { EntityWithId } from "@/hooks/use-entity-detail";
import { useEditReturn } from "@/hooks/use-edit-return";
import { useNewEntryParams } from "@/hooks/use-new-entry-params";
import type { ListRow } from "@/hooks/use-entity-list-page";
import type { EntityMetadata } from "@/lib/metadata/types";
import type { EditablePageDef } from "@/lib/page-def/types";
import { EntityEditBody } from "./entity-edit-body";
import { EntityEditHeader } from "./entity-edit-header";
import type { EditOutcome } from "./edit-outcome";

export interface EntityEditPageProps<
  Row extends ListRow,
  Values,
  Data extends EntityWithId,
  M extends EntityMetadata,
> {
  page: EditablePageDef<Row, Values, Data, M>;
  /** null adds a new entry: nothing is fetched and the form starts out blank. */
  id: number | null;
}

/**
 * The edit form of an entity on its own page: [EntityEditBody] wrapped in the page chrome and told
 * that every way it ends is a navigation. The shared body is where load, form, sections and save
 * live; this component adds only what makes it a *page* — the way back (`useEditReturn`), the new-entry
 * presets read from the URL (`useNewEntryParams`), and the [EditPageShell] with its breadcrumb header.
 *
 * The modal ([EntityEditModal]) is the same body with a different outcome and shell.
 */
export function EntityEditPage<
  Row extends ListRow,
  Values,
  Data extends EntityWithId,
  M extends EntityMetadata,
>({ page, id }: EntityEditPageProps<Row, Values, Data, M>) {
  const router = useRouter();
  const { edit } = page;
  // Where leaving the page leads: the caller that sent the user here, or the entity's own list.
  const back = useEditReturn({
    targets: edit.returnTargets,
    fallback: { route: page.route, labelKey: page.titleKey },
  });
  // What an "add" starts from: `/task/new?parentTaskId=42` presets the parent (see useNewEntryParams).
  const newParams = useNewEntryParams(edit.newEntryParams);

  // On a page every ending is a navigation — the one thing that differs from the modal (see
  // EditOutcome). Not memoized: the handlers are read when the user acts, never as an effect
  // dependency, so a fresh object each render costs nothing (as the page's own handlers always did).
  const outcome: EditOutcome = {
    afterSave: (savedId) =>
      router.push(
        savedId != null && back.savedRoute
          ? back.savedRoute(savedId)
          : back.route
      ),
    afterCancel: () => router.push(back.route),
    afterDelete: () => router.push(back.route),
    afterUndelete: () => router.push(back.route),
    afterClone: (route) => router.push(route),
  };

  return (
    <EntityEditBody
      page={page}
      id={id}
      newParams={newParams}
      outcome={outcome}
      renderShell={(regions) => (
        <EditPageShell
          header={
            <EntityEditHeader
              category={regions.category}
              listRoute={back.route}
              listLabel={back.label}
              title={regions.title}
              trailing={regions.trailing}
              crossLinks={regions.crossLinks}
              legacyUrl={regions.legacyUrl}
              deleted={regions.deleted}
            />
          }
          tabs={regions.tabs}
          tabPanels={regions.tabPanels}
          banner={regions.banner}
          sections={regions.sections}
          belowSections={regions.belowSections}
          actions={regions.actions}
        />
      )}
    />
  );
}
