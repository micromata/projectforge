"use client";

import { downloadLiquidityExcel } from "@/lib/rs/liquidity";
import type { MagicFilter } from "@/lib/rs/types";
import { ExcelExportButton } from "@/components/shared/excel-export-button";

/**
 * The Excel export of the liquidity list, as Wicket's list page offers it in its content menu: one row per
 * entry. It acts on the filter the list is showing (see PageDef.listActions), so it exports exactly the rows
 * the table shows.
 */
export function LiquidityListActions({ filter }: { filter: MagicFilter }) {
  return <ExcelExportButton download={() => downloadLiquidityExcel(filter)} />;
}
