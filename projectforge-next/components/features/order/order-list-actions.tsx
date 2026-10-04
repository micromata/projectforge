"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { toast } from "@/lib/toast";
import { HugeiconsIcon } from "@hugeicons/react";
import { Download04Icon } from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import { ExcelExportButton } from "@/components/shared/excel-export-button";
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
 *
 * The cache refresh of the finance staff is in the gear menu instead (see OrderGearMenuActions).
 */
export function OrderListActions({ filter }: { filter: MagicFilter }) {
  const t = useTranslations();
  const [forecastOpen, setForecastOpen] = useState(false);

  /**
   * The forecast dialog's reports, the same as [useExportDownload]'s: a filter matching nothing answers
   * 404, nothing was exported, and that is no error.
   */
  const reportEmpty = () => toast.info(t("datatable.no-records-found"));
  const reportError = (error: unknown) =>
    toast.error(error instanceof Error ? error.message : String(error));

  return (
    <>
      <ExcelExportButton download={() => downloadOrderExcel(filter)} />
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
