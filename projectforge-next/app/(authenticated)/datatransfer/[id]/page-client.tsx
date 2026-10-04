"use client";

import { notFound, useRouter } from "next/navigation";
import { useRouteParams } from "@/hooks/use-route-params";
import { PageShell } from "@/components/shared/page-shell";
import { EntityEditPage } from "@/components/shared/edit/entity-edit-page";
import { DATA_TRANSFER_PAGE } from "@/components/features/datatransfer/datatransfer.page";
import { DataTransferAreaPage } from "@/components/features/datatransfer/datatransfer-area-page";
import { PERSONAL_BOX_ID } from "@/lib/rs/datatransfer";

/**
 * `/datatransfer/{id}` is an area's page (files, info and admin form as tabs), `-1` the user's own
 * personal box — and `/datatransfer/new` the admin form of a new area, since one dynamic segment serves
 * both under the static export (a sibling `new/` would be a second shell for the same url pattern).
 */
export function DataTransferPageClient() {
  const router = useRouter();
  const raw = useRouteParams<{ id: string }>("/datatransfer/[id]")?.id;
  if (raw === undefined) return null;
  if (raw === "new") {
    return (
      <PageShell>
        <EntityEditPage
          page={DATA_TRANSFER_PAGE}
          id={null}
          // Into the new area's files, where the next thing to do — uploading — happens.
          outcome={{
            afterSave: (savedId) =>
              router.push(
                savedId != null ? `/datatransfer/${savedId}` : "/datatransfer"
              ),
          }}
        />
      </PageShell>
    );
  }
  const id = Number(raw);
  if (!Number.isInteger(id) || (id <= 0 && id !== PERSONAL_BOX_ID)) {
    notFound();
  }
  return <DataTransferAreaPage id={id} />;
}
