"use client";

import { MassUpdatePage } from "@/components/shared/list/mass-update-page";
import { SelectedEntriesPanel } from "@/components/shared/list/selected-entries-panel";
import { PROJECT_PAGE } from "@/components/features/project/project.page";
import { ProjectKost2ArtsMassUpdateField } from "@/components/features/project/project-kost2-arts-mass-update-field";

/**
 * Reached from the list, never linked directly: the selection this changes lives in the HTTP session,
 * and the list put it there before it routed here (see MassUpdatePage).
 */
export default function ProjectMassUpdatePage() {
  const massUpdate = PROJECT_PAGE.massUpdate!;
  return (
    <MassUpdatePage
      entity={PROJECT_PAGE.entity}
      massUpdate={massUpdate}
      listRoute={PROJECT_PAGE.route}
      listQueryKey={PROJECT_PAGE.queryKey}
      // The cost 2 types to create/activate or deactivate: several picks plus the action, which the generic
      // renderer has no field for (ProjectMultiSelectedPageRest declares `kost2Arts` as a custom field).
      customFields={{
        kost2Arts: (setParam, meta) => {
          const field = meta.fields.find((f) => f.field === "kost2Arts");
          return field ? (
            <ProjectKost2ArtsMassUpdateField meta={field} setParam={setParam} />
          ) : null;
        },
      }}
      // Built here rather than inside the generic page, because it renders the project list's own
      // columns — and those are typed, so only the page that declares them can pass them on.
      selectedEntries={(count) => (
        <SelectedEntriesPanel
          endpoint={massUpdate.endpoint}
          metadata={PROJECT_PAGE.metadata}
          columns={PROJECT_PAGE.columns}
          // The count is all this page knows of the selection, and it comes from `{page}/meta`, which
          // is refetched on every visit — so a selection changed elsewhere refetches the rows with it.
          selectionKey={String(count)}
          count={count}
        />
      )}
    />
  );
}
