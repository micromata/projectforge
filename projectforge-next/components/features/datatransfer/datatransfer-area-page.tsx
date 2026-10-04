"use client";

import { Activity, useEffect, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { useTranslations } from "next-intl";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { Button } from "@/components/ui/button";
import { EditPageTabs, TAB_PARAM } from "@/components/shared/edit-page-tabs";
import { GuardedLink } from "@/components/shared/guarded-link";
import { PageShell } from "@/components/shared/page-shell";
import { PageTitleRow } from "@/components/shared/page-title-row";
import { Spinner } from "@/components/shared/spinner";
import {
  dataTransferViewQueryKey,
  fetchDataTransferView,
  PERSONAL_BOX_ID,
} from "@/lib/rs/datatransfer";
import {
  DATA_TRANSFER_TAB,
  dataTransferTabs,
  resolveDataTransferTab,
} from "./datatransfer-area-tabs";
import { DataTransferEditTab } from "./datatransfer-edit-tab";
import { DataTransferFilesActions } from "./datatransfer-files-actions";
import { DataTransferFilesInfo } from "./datatransfer-files-info";
import { DataTransferFilesTab } from "./datatransfer-files-tab";

/**
 * One area (`/datatransfer/{id}`): its files, its info and — for who may change it — its admin form,
 * as tabs in `?tab=` so a tab can be linked and the back button works. The page every user with access
 * works on, admin or not.
 *
 * `-1` is the logged-in user's own personal box, which the backend creates on first use; the url is
 * replaced by the box's real id at once, so it can be bookmarked and passed on as any other area.
 */
export function DataTransferAreaPage({ id }: { id: number }) {
  const t = useTranslations();
  const router = useRouter();
  const params = useSearchParams();
  const queryClient = useQueryClient();
  const query = useQuery({
    queryKey: dataTransferViewQueryKey(id),
    queryFn: ({ signal }) => fetchDataTransferView(id, signal),
  });
  const view = query.data;
  const areaId = view?.area.id;
  const requested = params.get(TAB_PARAM);
  const activeTab = resolveDataTransferTab(
    requested,
    view?.editAccess === true
  );

  useEffect(() => {
    if (id !== PERSONAL_BOX_ID || areaId == null || !view) return;
    // Seeded, so the page under the new url doesn't load the box a second time.
    queryClient.setQueryData(dataTransferViewQueryKey(areaId), view);
    // Without `/next`: the router adds the base path itself. The tab goes along.
    const tab = requested ? `?${TAB_PARAM}=${requested}` : "";
    router.replace(`/datatransfer/${areaId}${tab}`);
  }, [id, areaId, view, requested, queryClient, router]);

  // The form is mounted on its first visit and then only hidden, so a look at the files keeps what is
  // being entered; saving or cancelling drops it (see DataTransferEditTab). Mounted on *entering* the
  // tab rather than whenever it is open: dropping it happens before the url leaves the tab.
  const [editMounted, setEditMounted] = useState(
    activeTab === DATA_TRANSFER_TAB.edit
  );
  const [lastTab, setLastTab] = useState(activeTab);
  if (lastTab !== activeTab) {
    setLastTab(activeTab);
    if (activeTab === DATA_TRANSFER_TAB.edit) setEditMounted(true);
  }

  const heading = t("plugins.datatransfer.title.heading");
  return (
    <PageShell>
      <PageTitleRow category={heading} title={view?.area.areaName ?? heading}>
        <Button asChild variant="outline" size="sm">
          <GuardedLink href="/datatransfer">{t("back")}</GuardedLink>
        </Button>
        {view && <DataTransferFilesActions view={view} />}
      </PageTitleRow>
      {query.isLoading ? (
        <div className="flex justify-center py-8">
          <Spinner className="h-5 w-5 border-2" />
        </div>
      ) : query.isError || !view || areaId == null ? (
        <p className="p-4 text-sm text-destructive">
          {query.error instanceof Error
            ? query.error.message
            : t("access.exception.noAccess")}
        </p>
      ) : (
        <div className="flex min-h-0 flex-1 flex-col overflow-hidden">
          <EditPageTabs
            tabs={dataTransferTabs(t, view.editAccess === true)}
            activeId={activeTab}
          />
          {activeTab === DATA_TRANSFER_TAB.files && (
            <DataTransferFilesTab view={view} />
          )}
          {activeTab === DATA_TRANSFER_TAB.info && (
            <div className="min-h-0 flex-1 overflow-y-auto p-4">
              <DataTransferFilesInfo view={view} />
            </div>
          )}
          {editMounted && view.editAccess && (
            <Activity
              mode={activeTab === DATA_TRANSFER_TAB.edit ? "visible" : "hidden"}
              name="datatransfer-edit"
            >
              <DataTransferEditTab
                id={areaId}
                onLeave={() => setEditMounted(false)}
              />
            </Activity>
          )}
        </div>
      )}
    </PageShell>
  );
}
