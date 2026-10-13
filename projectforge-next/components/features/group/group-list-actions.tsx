"use client";

import { downloadListExcel } from "@/lib/rs/list-export";
import type { MagicFilter } from "@/lib/rs/types";
import { ExcelExportMenu } from "@/components/shared/export-menu";
import { useAuth } from "@/hooks/use-auth";

/**
 * The Excel export of the group list, as `GroupEntityRest` offers it
 * (`layout.excelExportSupported` for an administrator).
 *
 * Acts on the filter the list is showing, which is why it lives in its toolbar and is handed that
 * filter (see PageDef.listActions).
 *
 * Offered to administrators only, the same condition the endpoint checks itself
 * (`accessChecker.checkIsLoggedInUserMemberOfAdminGroup`) — a menu that can only fail is worse than
 * no menu.
 */
export function GroupListActions({ filter }: { filter: MagicFilter }) {
  const { isAdmin } = useAuth();
  if (!isAdmin) return null;

  return (
    <ExcelExportMenu download={() => downloadListExcel("group", filter)} />
  );
}
