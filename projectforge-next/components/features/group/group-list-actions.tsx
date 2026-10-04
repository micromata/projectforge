"use client";

import { downloadListExcel } from "@/lib/rs/list-export";
import type { MagicFilter } from "@/lib/rs/types";
import { ExcelExportButton } from "@/components/shared/excel-export-button";
import { useAuth } from "@/hooks/use-auth";

/**
 * The Excel export of the group list, as `GroupPagesRest` offers it
 * (`layout.excelExportSupported` for an administrator).
 *
 * Acts on the filter the list is showing, which is why it lives in its toolbar and is handed that
 * filter (see PageDef.listActions).
 *
 * Offered to administrators only, the same condition the endpoint checks itself
 * (`accessChecker.checkIsLoggedInUserMemberOfAdminGroup`) — a button that can only fail is worse than
 * no button.
 */
export function GroupListActions({ filter }: { filter: MagicFilter }) {
  const { isAdmin } = useAuth();
  if (!isAdmin) return null;

  return (
    <ExcelExportButton download={() => downloadListExcel("group", filter)} />
  );
}
