"use client";

import { notFound, useRouter } from "next/navigation";
import { useRouteParams } from "@/hooks/use-route-params";
import { PageShell } from "@/components/shared/page-shell";
import { EntityEditPage } from "@/components/shared/edit/entity-edit-page";
import { DATA_TRANSFER_PAGE } from "@/components/features/datatransfer/datatransfer.page";

/**
 * The admin form of an existing area. It is opened from the area's file view, so saving and cancelling
 * lead back there; a deleted area has no files to return to, which leaves the default (the list).
 */
export function DataTransferEditPageClient() {
  const router = useRouter();
  const raw = useRouteParams<{ id: string }>("/datatransfer/[id]/edit")?.id;
  if (raw === undefined) return null;
  const id = Number(raw);
  if (!Number.isInteger(id) || id <= 0) notFound();
  const filesRoute = `/datatransfer/${id}`;

  return (
    <PageShell>
      <EntityEditPage
        page={DATA_TRANSFER_PAGE}
        id={id}
        outcome={{
          afterSave: () => router.push(filesRoute),
          afterCancel: () => router.push(filesRoute),
        }}
      />
    </PageShell>
  );
}
