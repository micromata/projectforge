"use client";

import { useListMeta } from "@/hooks/use-list-meta";
import { downloadListExcel } from "@/lib/rs/list-export";
import type { MagicFilter } from "@/lib/rs/types";
import { ExcelExportMenu } from "@/components/shared/export-menu";

/**
 * The Excel export of the selected campaign (the addresses with their values), for the finance and marketing
 * staff only, as the React list offered it: whether it is offered is the backend's answer
 * (`AddressCampaignValueEntityRest.addVariablesForListPage`), which checks the export itself as well.
 */
export function AddressCampaignValueListActions({
  filter,
}: {
  filter: MagicFilter;
}) {
  const allowed =
    useListMeta("addressCampaignValue").data?.variables
      ?.excelExportSupported === true;
  if (!allowed) return null;
  return (
    <ExcelExportMenu
      download={() => downloadListExcel("addressCampaignValue", filter)}
    />
  );
}
