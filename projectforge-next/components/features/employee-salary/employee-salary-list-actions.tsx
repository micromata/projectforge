"use client";

import { useTranslations } from "next-intl";
import { useMutation } from "@tanstack/react-query";
import { toast } from "@/lib/toast";
import { HugeiconsIcon } from "@hugeicons/react";
import { Download04Icon } from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import { Spinner } from "@/components/shared/spinner";
import { RsError } from "@/lib/rs/client";
import { downloadEmployeeSalaryExcel } from "@/lib/rs/employee-salary";
import type { MagicFilter } from "@/lib/rs/types";

/**
 * The Excel export Wicket's salary list offers in its content menu, acting on the filter the list is
 * showing (hence its place in the toolbar and the filter it is handed — see PageDef.listActions).
 *
 * The backend requires a month be picked and refuses otherwise with 400 and the reason as its body
 * (`fibu.employee.salary.error.monthNotGiven`), which is shown as it is; an empty result answers 404,
 * which is no error but "nothing to export".
 */
export function EmployeeSalaryListActions({ filter }: { filter: MagicFilter }) {
  const t = useTranslations();

  const excel = useMutation({
    mutationFn: () => downloadEmployeeSalaryExcel(filter),
    onError: (error) => {
      // 404: the filter matched nothing — nothing was exported, and that is no error.
      if (error instanceof RsError && error.status === 404) {
        toast.info(t("datatable.no-records-found"));
        return;
      }
      // 400 carries the backend's own message (no month picked); everything else its status line.
      toast.error(error instanceof Error ? error.message : String(error));
    },
  });

  return (
    <HintTooltip text={t("fibu.employee.salary.exportXls.tooltip")}>
      <Button
        type="button"
        variant="ghost"
        size="sm"
        className="gap-1.5"
        onClick={() => excel.mutate()}
        disabled={excel.isPending}
      >
        {excel.isPending ? (
          <Spinner className="h-3.5 w-3.5 border-2" />
        ) : (
          <HugeiconsIcon icon={Download04Icon} size={14} aria-hidden />
        )}
        {t("exportAsXls")}
      </Button>
    </HintTooltip>
  );
}
