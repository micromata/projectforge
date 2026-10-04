"use client";

import { useEffect } from "react";
import { notFound, useRouter } from "next/navigation";
import { useRouteParams } from "@/hooks/use-route-params";
import { DATA_TRANSFER_TAB } from "@/components/features/datatransfer/datatransfer-area-tabs";
import { TAB_PARAM } from "@/components/shared/edit-page-tabs";

/**
 * The admin form of an existing area is a tab of its page; this url only forwards there. Kept because
 * `NextMigration.editRoute` (old `react/datatransfer/edit/{id}` links) and bookmarks still name it.
 */
export function DataTransferEditPageClient() {
  const router = useRouter();
  const raw = useRouteParams<{ id: string }>("/datatransfer/[id]/edit")?.id;
  const id = raw === undefined ? undefined : Number(raw);
  const valid = id !== undefined && Number.isInteger(id) && id > 0;

  useEffect(() => {
    if (valid) {
      router.replace(
        `/datatransfer/${id}?${TAB_PARAM}=${DATA_TRANSFER_TAB.edit}`
      );
    }
  }, [valid, id, router]);

  if (raw !== undefined && !valid) notFound();
  return null;
}
