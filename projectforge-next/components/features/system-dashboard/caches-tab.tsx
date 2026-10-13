"use client";

import { useCallback, useState } from "react";
import { useTranslations } from "next-intl";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ConfirmDialog } from "@/components/shared/confirm-dialog";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import { Button } from "@/components/ui/button";
import { TabsContent } from "@/components/ui/tabs";
import { isAccessDenied } from "@/hooks/use-read-access-guard";
import {
  type CacheEntry,
  fetchAdminCaches,
  refreshAdminCache,
  refreshAllAdminCaches,
  terminateAdminCacheRefresh,
} from "@/lib/rs/admin-caches";
import { toast } from "@/lib/toast";
import { isRefreshing } from "./cache-labels";
import { CachesTable } from "./caches-table";

/** React Query key of the caches. */
export const CACHES_QUERY_KEY = "adminCaches";

/**
 * The caches tab of the system dashboard (`?tab=caches`, admin group only): every cache with its state, last
 * refresh, duration and error, refreshed every 5 seconds — every 2 seconds while a refresh is running. A cache, or
 * all of them, can be refreshed by hand; the refresh runs in the background. A running refresh (e.g. hanging on a
 * database lock) can be terminated.
 */
export function CachesTab() {
  return (
    <TabsContent value="caches" className="flex min-h-0 flex-col text-sm">
      <Caches />
    </TabsContent>
  );
}

/** Mounted with the open tab only, so the caches are fetched once it is shown. */
function Caches() {
  const t = useTranslations();
  const queryClient = useQueryClient();
  const list = useQuery({
    queryKey: [CACHES_QUERY_KEY, "list"],
    queryFn: ({ signal }) => fetchAdminCaches(signal),
    refetchInterval: (query) =>
      isRefreshing(query.state.data) ? 2_000 : 5_000,
  });
  // The refresh starts in the background: shown as running a moment later.
  const refetchSoon = () =>
    setTimeout(
      () => queryClient.invalidateQueries({ queryKey: [CACHES_QUERY_KEY] }),
      500
    );
  const onError = (err: unknown) =>
    toast.error(err instanceof Error ? err.message : String(err));
  const refresh = useMutation({
    mutationFn: (id: string) => refreshAdminCache(id),
    onSuccess: (caches) => {
      queryClient.setQueryData([CACHES_QUERY_KEY, "list"], caches);
      toast.success(t("system.caches.refresh.started"));
      refetchSoon();
    },
    onError,
  });
  const refreshAll = useMutation({
    mutationFn: refreshAllAdminCaches,
    onSuccess: (started) => {
      if (started) {
        toast.success(t("system.caches.refreshAll.started"));
      } else {
        toast.error(t("system.caches.refreshAll.running"));
      }
      refetchSoon();
    },
    onError,
  });
  const terminate = useMutation({
    mutationFn: (id: string) => terminateAdminCacheRefresh(id),
    onSuccess: (terminated) => {
      if (terminated) {
        toast.success(t("system.caches.terminate.done"));
      } else {
        toast.error(t("system.caches.terminate.notRunning"));
      }
      queryClient.invalidateQueries({ queryKey: [CACHES_QUERY_KEY] });
    },
    onError,
  });
  // The cache whose refresh is to be terminated, once confirmed.
  const [toTerminate, setToTerminate] = useState<CacheEntry>();
  // Stable, so the table's columns are not rebuilt on every poll.
  const { mutate } = refresh;
  const refreshCache = useCallback(
    (cache: CacheEntry) => mutate(cache.id),
    [mutate]
  );
  const data = list.data;
  if (!data) {
    return (
      <div className="pb-8 pt-2">
        {isAccessDenied(list.error) ? (
          <p className="text-destructive">{t("access.exception.noAccess")}</p>
        ) : list.isError ? (
          <p className="text-destructive">{t("errorpage.title")}</p>
        ) : (
          <p className="text-muted-foreground">{t("loading")}</p>
        )}
      </div>
    );
  }
  return (
    <>
      <CachesTable
        caches={data}
        isFetching={list.isFetching}
        refreshing={refresh.isPending ? refresh.variables : undefined}
        onRefresh={refreshCache}
        onTerminate={setToTerminate}
        toolbar={
          <HintTooltip text={t("system.caches.refreshAll.tooltip")}>
            <Button
              size="sm"
              variant="outline"
              disabled={refreshAll.isPending}
              onClick={() => refreshAll.mutate()}
            >
              {t("system.caches.refreshAll._")}
            </Button>
          </HintTooltip>
        }
      />
      <ConfirmDialog
        open={!!toTerminate}
        onOpenChange={(open) => !open && setToTerminate(undefined)}
        title={t("system.caches.terminate._")}
        description={t("system.caches.terminate.question", {
          arg0: toTerminate?.status.name ?? "",
        })}
        confirmLabel={t("system.caches.terminate._")}
        destructive
        onConfirm={() => {
          if (toTerminate) terminate.mutate(toTerminate.id);
          setToTerminate(undefined);
        }}
      />
    </>
  );
}
