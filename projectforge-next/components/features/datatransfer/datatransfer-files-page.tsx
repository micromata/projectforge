"use client";

import { useEffect } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useTranslations } from "next-intl";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { Button } from "@/components/ui/button";
import { AttachmentList } from "@/components/shared/attachments/attachment-list";
import { PageShell } from "@/components/shared/page-shell";
import { PageTitleRow } from "@/components/shared/page-title-row";
import { Spinner } from "@/components/shared/spinner";
import {
  dataTransferViewQueryKey,
  fetchDataTransferView,
  PERSONAL_BOX_ID,
} from "@/lib/rs/datatransfer";
import { DataTransferFilesActions } from "./datatransfer-files-actions";
import { DataTransferFilesInfo } from "./datatransfer-files-info";

/**
 * The files of one area (`/datatransfer/{id}`) — the page every user with access works on, admin or
 * not; the admin form is a page of its own behind the edit button.
 *
 * `-1` is the logged-in user's own personal box, which the backend creates on first use; the url is
 * replaced by the box's real id at once, so it can be bookmarked and passed on as any other area.
 */
export function DataTransferFilesPage({ id }: { id: number }) {
  const t = useTranslations();
  const router = useRouter();
  const queryClient = useQueryClient();
  const query = useQuery({
    queryKey: dataTransferViewQueryKey(id),
    queryFn: ({ signal }) => fetchDataTransferView(id, signal),
  });
  const view = query.data;
  const areaId = view?.area.id;

  useEffect(() => {
    if (id !== PERSONAL_BOX_ID || areaId == null || !view) return;
    // Seeded, so the page under the new url doesn't load the box a second time.
    queryClient.setQueryData(dataTransferViewQueryKey(areaId), view);
    // Without `/next`: the router adds the base path itself.
    router.replace(`/datatransfer/${areaId}`);
  }, [id, areaId, view, queryClient, router]);

  const heading = t("plugins.datatransfer.title.heading");
  const back = (
    <Button asChild variant="outline" size="sm">
      <Link href="/datatransfer">{t("back")}</Link>
    </Button>
  );

  return (
    <PageShell>
      <PageTitleRow category={heading} title={view?.area.areaName ?? heading}>
        {back}
        {view && <DataTransferFilesActions view={view} />}
      </PageTitleRow>
      <div className="flex min-h-0 flex-1 flex-col gap-4 overflow-y-auto p-4">
        {query.isLoading ? (
          <div className="flex justify-center py-8">
            <Spinner className="h-5 w-5 border-2" />
          </div>
        ) : query.isError || !view || areaId == null ? (
          <p className="text-sm text-destructive">
            {query.error instanceof Error
              ? query.error.message
              : t("access.exception.noAccess")}
          </p>
        ) : (
          <>
            <AttachmentList
              entity="datatransfer"
              id={areaId}
              uploadHint={`${t("plugins.datatransfer.maxUploadSize._")}: ${
                view.area.capacity?.maxUploadSizeFormatted ?? ""
              }`}
              // Size, capacity and the "download all" depend on the files.
              onChanged={() =>
                void queryClient.invalidateQueries({
                  queryKey: dataTransferViewQueryKey(areaId),
                })
              }
            />
            <DataTransferFilesInfo view={view} />
          </>
        )}
      </div>
    </PageShell>
  );
}
