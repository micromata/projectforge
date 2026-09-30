"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "@/lib/toast";
import { HugeiconsIcon } from "@hugeicons/react";
import { Download04Icon, RefreshIcon } from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import { Spinner } from "@/components/shared/spinner";
import { RsError } from "@/lib/rs/client";
import {
  downloadOrderExcel,
  fetchRefreshCacheAccess,
  refreshOrderCache,
  REFRESH_CACHE_ACCESS_QUERY_KEY,
} from "@/lib/rs/order";
import type { MagicFilter } from "@/lib/rs/types";
import { ForecastExportDialog } from "./forecast-export-dialog";
import { ORDER_LIST_QUERY_KEY } from "./order.page";

/**
 * The two exports of the order book, as Wicket's list page offers them in its content menu: the list
 * itself as Excel, and the forecast.
 *
 * Both act on the filter the list is showing, which is why they live in its toolbar and are handed that
 * filter (see PageDef.listActions). The forecast asks for its start month first — see
 * [ForecastExportDialog].
 *
 * The finance staff additionally gets a button to rebuild the order and invoice caches, a manual fallback
 * for when the invoiced sums of an order don't reflect a just changed invoice yet.
 */
export function OrderListActions({ filter }: { filter: MagicFilter }) {
  const t = useTranslations();
  const [forecastOpen, setForecastOpen] = useState(false);

  /** A filter matching nothing answers 404: nothing was exported, and that is no error. */
  const reportEmpty = () => toast.info(t("datatable.no-records-found"));
  const reportError = (error: unknown) =>
    toast.error(error instanceof Error ? error.message : String(error));

  const queryClient = useQueryClient();
  const refreshCacheAccess = useQuery({
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
    onError: reportError,
  });

  const excel = useMutation({
    mutationFn: () => downloadOrderExcel(filter),
    onError: (error) => {
      if (error instanceof RsError && error.status === 404) {
        reportEmpty();
        return;
      }
      reportError(error);
    },
  });

  return (
    <>
      <HintTooltip text={t("tooltip.export.excel")}>
        <Button
          type="button"
          variant="ghost"
          size="sm"
          className="gap-1.5"
          onClick={() => excel.mutate()}
          disabled={excel.isPending}
        >
          {excel.isPending ? (
            <Spinner className="h-3.5 w-3.5 border-2" />
          ) : (
            <HugeiconsIcon icon={Download04Icon} size={14} aria-hidden />
          )}
          {t("exportAsXls")}
        </Button>
      </HintTooltip>
      <HintTooltip text={t("fibu.auftrag.forecastExport.tooltip")}>
        <Button
          type="button"
          variant="ghost"
          size="sm"
          className="gap-1.5"
          onClick={() => setForecastOpen(true)}
        >
          <HugeiconsIcon icon={Download04Icon} size={14} aria-hidden />
          {t("fibu.auftrag.forecastExportAsXls._")}
        </Button>
      </HintTooltip>
      {refreshCacheAccess.data?.access && (
        <HintTooltip text={t("fibu.auftrag.refreshCache.tooltip")}>
          <Button
            type="button"
            variant="ghost"
            size="sm"
            className="gap-1.5"
            onClick={() => refreshCache.mutate()}
            disabled={refreshCache.isPending}
          >
            {refreshCache.isPending ? (
              <Spinner className="h-3.5 w-3.5 border-2" />
            ) : (
              <HugeiconsIcon icon={RefreshIcon} size={14} aria-hidden />
            )}
            {t("fibu.auftrag.refreshCache._")}
          </Button>
        </HintTooltip>
      )}
      {forecastOpen && (
        <ForecastExportDialog
          filter={filter}
          onClose={() => setForecastOpen(false)}
          onEmptyResult={reportEmpty}
          onError={reportError}
        />
      )}
    </>
  );
}
