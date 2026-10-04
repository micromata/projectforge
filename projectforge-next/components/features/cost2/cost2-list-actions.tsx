"use client";

import { useTranslations } from "next-intl";
import { useMutation } from "@tanstack/react-query";
import { toast } from "@/lib/toast";
import { RsError } from "@/lib/rs/client";
import { downloadListExcel } from "@/lib/rs/list-export";
import type { MagicFilter } from "@/lib/rs/types";
import { ExportButton } from "@/components/shared/export-button";

/**
 * The Excel export of the cost 2 list, as the legacy Wicket `Kost2ListPage` offered it
 * ("exportAsXls"; `Kost2EntityRest.exportAsExcel`).
 *
 * Acts on the filter the list is showing, which is why it lives in its toolbar and is handed that
 * filter (see PageDef.listActions). Select access is all the endpoint asks for, so there is no gate.
 */
export function Cost2ListActions({ filter }: { filter: MagicFilter }) {
  const t = useTranslations();

  const excel = useMutation({
    mutationFn: () => downloadListExcel("cost2", filter),
    // A 404 is no error here: the filter matched nothing.
    onError: (error: unknown) => {
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
