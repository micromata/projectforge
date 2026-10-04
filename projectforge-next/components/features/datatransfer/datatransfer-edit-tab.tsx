"use client";

import { useRouter } from "next/navigation";
import { useQueryClient } from "@tanstack/react-query";
import { EntityEditBody } from "@/components/shared/edit/entity-edit-body";
import { dataTransferViewQueryKey } from "@/lib/rs/datatransfer";
import { DATA_TRANSFER_PAGE } from "./datatransfer.page";

/**
 * The admin form of an area, as a tab of its page.
 *
 * The shared form body without a page around it: the page's header already names the area, so this
 * lays out only the sections and the pinned action bar (like EntityEditDialogShell, minus the dialog).
 * Saving and cancelling both end in the files tab — `onLeave` lets the page drop the form, so the next
 * visit starts from what is stored rather than from abandoned input. A deleted area has no page left,
 * which leaves the list.
 */
export function DataTransferEditTab({
  id,
  onLeave,
}: {
  id: number;
  onLeave: () => void;
}) {
  const router = useRouter();
  const queryClient = useQueryClient();
  const filesRoute = `/datatransfer/${id}`;
  const leave = () => {
    onLeave();
    router.replace(filesRoute);
  };

  return (
    <EntityEditBody
      page={DATA_TRANSFER_PAGE}
      id={id}
      outcome={{
        afterSave: () => {
          // The view is a read of its own (DataTransferFilesRest), not the form's entity.
          void queryClient.invalidateQueries({
            queryKey: dataTransferViewQueryKey(id),
          });
          leave();
        },
        afterCancel: leave,
        afterDelete: () => router.push("/datatransfer"),
        afterUndelete: leave,
        afterClone: (route) => router.push(route),
      }}
      renderShell={(regions) => (
        <>
          {regions.banner && <div className="shrink-0">{regions.banner}</div>}
          <div className="min-h-0 flex-1 overflow-y-auto bg-muted/30 px-4 pb-4">
            {regions.aboveSections && (
              <div className="pt-4">{regions.aboveSections}</div>
            )}
            {regions.sections.map((section, i) => (
              <div key={regions.tabs[i]?.id ?? i} className="pt-4">
                {typeof section === "function" ? section(false) : section}
              </div>
            ))}
            {regions.belowSections && (
              <div className="pt-4">{regions.belowSections}</div>
            )}
          </div>
          <div className="shrink-0">{regions.actions}</div>
        </>
      )}
    />
  );
}
