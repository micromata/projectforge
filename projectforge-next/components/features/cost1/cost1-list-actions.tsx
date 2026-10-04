"use client";

import { downloadListExcel } from "@/lib/rs/list-export";
import type { MagicFilter } from "@/lib/rs/types";
import { ExcelExportButton } from "@/components/shared/excel-export-button";

/**
 * The Excel export of the cost 1 list, as the legacy Wicket `Kost1ListPage` offered it
 * ("exportAsXls"; `Kost1EntityRest.exportAsExcel`).
 *
 * Acts on the filter the list is showing, which is why it lives in its toolbar and is handed that
 * filter (see PageDef.listActions). Select access is all the endpoint asks for, so there is no gate.
 */
export function Cost1ListActions({ filter }: { filter: MagicFilter }) {
  return (
    <ExcelExportButton download={() => downloadListExcel("cost1", filter)} />
  );
}
