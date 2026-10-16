"use client";

import { MassUpdatePage } from "@/components/shared/list/mass-update-page";
import { SelectedEntriesPanel } from "@/components/shared/list/selected-entries-panel";
import { ADDRESS_PAGE } from "@/components/features/address/address.page";

/**
 * Reached from the list, never linked directly: the selection this changes lives in the HTTP session, and the
 * list put it there before it routed here (see MassUpdatePage).
 */
export default function AddressMassUpdatePage() {
  const massUpdate = ADDRESS_PAGE.massUpdate!;
  return (
    <MassUpdatePage
      entity={ADDRESS_PAGE.entity}
      massUpdate={massUpdate}
      listRoute={ADDRESS_PAGE.route}
      listQueryKey={ADDRESS_PAGE.queryKey}
      selectedEntries={(count) => (
        <SelectedEntriesPanel
          endpoint={massUpdate.endpoint}
          metadata={ADDRESS_PAGE.metadata}
          columns={ADDRESS_PAGE.columns}
          selectionKey={String(count)}
          count={count}
        />
      )}
    />
  );
}
