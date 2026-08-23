"use client";

import { useTranslations } from "next-intl";
import { useMutation } from "@tanstack/react-query";
import { toast } from "@/lib/toast";
import { RsError } from "@/lib/rs/client";
import { downloadListExcel } from "@/lib/rs/list-export";
import type { MagicFilter } from "@/lib/rs/types";
import { ExportButton } from "@/components/shared/export-button";
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
  const t = useTranslations();
  const { isAdmin } = useAuth();

  const excel = useMutation({
    mutationFn: () => downloadListExcel("group", filter),
    // A 404 is no error here: the filter matched nothing.
    onError: (error: unknown) => {
      if (error instanceof RsError && error.status === 404) {
        toast.info(t("datatable.no-records-found"));
        return;
      }
      toast.error(error instanceof Error ? error.message : String(error));
    },
  });

  if (!isAdmin) return null;

  return (
    <ExportButton
      tooltip={t("tooltip.export.excel")}
      label={t("exportAsXls")}
      isPending={excel.isPending}
      onClick={() => excel.mutate()}
    />
  );
}
