"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { useMutation } from "@tanstack/react-query";
import { toast } from "sonner";
import { HugeiconsIcon } from "@hugeicons/react";
import { Download04Icon } from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import { Spinner } from "@/components/shared/spinner";
import { RsError } from "@/lib/rs/client";
import { downloadOrderExcel } from "@/lib/rs/order";
import type { MagicFilter } from "@/lib/rs/types";
import { ForecastExportDialog } from "./forecast-export-dialog";

/**
 * The two exports of the order book, as Wicket's list page offers them in its content menu: the list
 * itself as Excel, and the forecast.
 *
 * Both act on the filter the list is showing, which is why they live in its toolbar and are handed that
 * filter (see PageDef.listActions). The forecast asks for its start month first — see
 * [ForecastExportDialog].
 */
export function OrderListActions({ filter }: { filter: MagicFilter }) {
  const t = useTranslations();
  const [forecastOpen, setForecastOpen] = useState(false);

  /** A filter matching nothing answers 404: nothing was exported, and that is no error. */
  const reportEmpty = () => toast.info(t("datatable.no-records-found"));
  const reportError = (error: unknown) =>
    toast.error(error instanceof Error ? error.message : String(error));

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
      <Button
        type="button"
        variant="ghost"
        size="sm"
        className="gap-1.5"
        title={t("tooltip.export.excel")}
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
      <Button
        type="button"
        variant="ghost"
        size="sm"
        className="gap-1.5"
        title={t("fibu.auftrag.forecastExport.tooltip")}
        onClick={() => setForecastOpen(true)}
      >
        <HugeiconsIcon icon={Download04Icon} size={14} aria-hidden />
        {t("fibu.auftrag.forecastExportAsXls._")}
      </Button>
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
