"use client";

import type { ListSelection } from "@/hooks/use-list-selection";
import type { ListRow } from "@/hooks/use-entity-list-page";
import type { EntityMetadata } from "@/lib/metadata/types";
import type { ColumnDeclaration, MassUpdateDef } from "@/lib/page-def/types";
import { MassUpdateButton } from "./mass-update-button";
import { SelectedEntriesPanel } from "./selected-entries-panel";
import { SelectionBar } from "./selection-bar";

/**
 * What a list shows above its table while it is in selection mode: the bar of counts and actions, and
 * below it the collapsible list of the picked entries.
 *
 * Its own component, so [EntityListPage] states this once — the two belong together and are shown or
 * hidden as one.
 */
export function ListSelectionSection<
  Row extends ListRow,
  M extends EntityMetadata,
>({
  massUpdate,
  mode,
  metadata,
  columns,
  onSelectAll,
}: {
  massUpdate: MassUpdateDef;
  mode: ListSelection;
  metadata: M;
  /** The list's own column declarations, so the panel's rows read as the table's do. */
  columns: ColumnDeclaration<Row, M>[];
  onSelectAll: () => void;
}) {
  return (
    <>
      <SelectionBar
        count={mode.selectedIds.length}
        onSelectAll={onSelectAll}
        onClear={() => mode.selection?.clear()}
        onLeave={mode.leave}
        actions={
          <MassUpdateButton
            massUpdate={massUpdate}
            selectedIds={mode.selectedIds}
            flush={mode.flush}
          />
        }
      />
      {mode.selectedIds.length > 0 && (
        <SelectedEntriesPanel<Row, M>
          endpoint={massUpdate.endpoint}
          metadata={metadata}
          columns={columns}
          // The ticks themselves, because they are what changed: the panel is open while the user keeps
          // picking, and every change has to reach it.
          selectionKey={mode.selectedIds.join(",")}
          count={mode.selectedIds.length}
          // The ticks are posted debounced, and this reads them back from the session — so the write
          // has to have landed first (see ListSelection.flush).
          beforeFetch={mode.flush}
          className="mx-4 mb-1"
        />
      )}
    </>
  );
}
