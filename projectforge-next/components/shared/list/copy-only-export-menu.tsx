"use client";

import { useCopyTableContext } from "@/components/data-table/copy-table-context";
import { ExportMenu } from "@/components/shared/export-menu";

/**
 * The export menu of a list whose own actions bring none (or none for this user): it holds only the entry
 * copying the table. Gone as soon as one of the list's actions renders an [ExportMenu], which carries that
 * entry itself.
 */
export function CopyOnlyExportMenu() {
  const copyTable = useCopyTableContext();
  if (!copyTable || copyTable.hasExportMenu) return null;
  return <ExportMenu fallback />;
}
