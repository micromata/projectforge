"use client";

import { downloadListExcel } from "@/lib/rs/list-export";
import type { MagicFilter } from "@/lib/rs/types";
import { ExcelExportMenu } from "@/components/shared/export-menu";
import { useAuth } from "@/hooks/use-auth";

/**
 * The Excel export of the user list (`UserEntityRest.exportAsExcel`), acting on the filter the list is
 * showing. Offered to administrators only, the same condition the endpoint checks itself.
 */
export function UserListActions({ filter }: { filter: MagicFilter }) {
  const { isAdmin } = useAuth();
  if (!isAdmin) return null;

  return <ExcelExportMenu download={() => downloadListExcel("user", filter)} />;
}
