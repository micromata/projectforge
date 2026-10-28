"use client";

import { downloadListExcel } from "@/lib/rs/list-export";
import type { MagicFilter } from "@/lib/rs/types";
import { ExcelExportMenu } from "@/components/shared/export-menu";

/** The Excel export of the list (`EmployeeEntityRest.exportAsExcel`), filtered as the list is. */
export function EmployeeListActions({ filter }: { filter: MagicFilter }) {
  return (
    <ExcelExportMenu download={() => downloadListExcel("employee", filter)} />
  );
}
