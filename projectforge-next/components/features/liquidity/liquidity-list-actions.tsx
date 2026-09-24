"use client";

import { useTranslations } from "next-intl";
import { useMutation } from "@tanstack/react-query";
import { toast } from "@/lib/toast";
import { RsError } from "@/lib/rs/client";
import { downloadLiquidityExcel } from "@/lib/rs/liquidity";
import type { MagicFilter } from "@/lib/rs/types";
import { ExportButton } from "@/components/shared/export-button";

/**
 * The Excel export of the liquidity list, as Wicket's list page offers it in its content menu: one row per
 * entry. It acts on the filter the list is showing (see PageDef.listActions), so it exports exactly the rows
 * the table shows.
 */
export function LiquidityListActions({ filter }: { filter: MagicFilter }) {
  const t = useTranslations();

  const excel = useMutation({
    mutationFn: () => downloadLiquidityExcel(filter),
    onError: (error: unknown) => {
      // A 404 is no error here: the filter matched nothing, so the export has nothing to write.
      if (error instanceof RsError && error.status === 404) {
        toast.info(t("datatable.no-records-found"));
        return;
      }
      toast.error(error instanceof Error ? error.message : String(error));
    },
  });

  return (
    <ExportButton
      tooltip={t("tooltip.export.excel")}
      label={t("exportAsXls")}
      isPending={excel.isPending}
      onClick={() => excel.mutate()}
    />
  );
}
