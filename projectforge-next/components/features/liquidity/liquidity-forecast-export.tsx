"use client";

import { useTranslations } from "next-intl";
import { useMutation } from "@tanstack/react-query";
import { toast } from "@/lib/toast";
import {
  downloadLiquidityForecastExcel,
  type LiquidityForecastRequest,
} from "@/lib/rs/liquidity";
import { ExportButton } from "@/components/shared/export-button";

/**
 * The Excel export of the forecast tab: cash flow per day, all entries the forecast is based on and the
 * debitor and creditor invoices, for exactly the [params] the charts are showing.
 */
export function LiquidityForecastExport({
  params,
  disabled,
}: {
  params: LiquidityForecastRequest;
  disabled?: boolean;
}) {
  const t = useTranslations();

  const excel = useMutation({
    mutationFn: () => downloadLiquidityForecastExcel(params),
    onError: (error: unknown) => {
      toast.error(error instanceof Error ? error.message : String(error));
    },
  });

  return (
    <ExportButton
      tooltip={t("tooltip.export.excel")}
      label={t("exportAsXls")}
      isPending={excel.isPending}
      disabled={disabled}
      onClick={() => excel.mutate()}
    />
  );
}
