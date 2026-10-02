"use client";

import { useTranslations } from "next-intl";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "@/lib/toast";
import { GearMenuItem } from "@/components/data-table";
import {
  fetchRefreshCacheAccess,
  refreshOrderCache,
  REFRESH_CACHE_ACCESS_QUERY_KEY,
} from "@/lib/rs/order";
import { ORDER_LIST_QUERY_KEY } from "./order.page";

/**
 * The order book's own entry of the gear menu (see PageDef.gearMenuActions): rebuilding the order and
 * invoice caches, a manual fallback for when the invoiced sums of an order don't reflect a just changed
 * invoice yet. Only for the finance staff — the backend says who that is.
 */
export function OrderGearMenuActions() {
  const t = useTranslations();
  const queryClient = useQueryClient();
  const access = useQuery({
    queryKey: REFRESH_CACHE_ACCESS_QUERY_KEY,
    queryFn: ({ signal }) => fetchRefreshCacheAccess(signal),
    staleTime: Infinity,
  });
  const refreshCache = useMutation({
    mutationFn: refreshOrderCache,
    onSuccess: ({ message }) => {
      toast.success(message);
      // The list shows the sums of the rebuilt caches only after reloading.
      void queryClient.invalidateQueries({ queryKey: ORDER_LIST_QUERY_KEY });
    },
    onError: (error) =>
      toast.error(error instanceof Error ? error.message : String(error)),
  });

  if (!access.data?.access) return null;
  return (
    <GearMenuItem
      label={t("fibu.auftrag.refreshCache._")}
      description={t("fibu.auftrag.refreshCache.tooltip")}
      disabled={refreshCache.isPending}
      onSelect={() => refreshCache.mutate()}
    />
  );
}
