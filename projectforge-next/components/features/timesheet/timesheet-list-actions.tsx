"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { useMutation } from "@tanstack/react-query";
import { HugeiconsIcon } from "@hugeicons/react";
import { Calendar03Icon, Download04Icon } from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import { Spinner } from "@/components/shared/spinner";
import { toast } from "@/lib/toast";
import { downloadTimesheetExcel } from "@/lib/rs/timesheet";
import type { MagicFilter } from "@/lib/rs/types";
import { TimesheetIcsDialog } from "./timesheet-ics-dialog";

/**
 * The two exports of the time sheet list the legacy list offers in its content menu (minus the PDF,
 * which is still coupled to the Wicket module): the filtered list as Excel, and the ics subscription url.
 *
 * The Excel export acts on the filter the list is showing, which is why it lives in the toolbar and is
 * handed that filter (see PageDef.listActions). The ics url is the user's own and opens a dialog (see
 * TimesheetIcsDialog).
 */
export function TimesheetListActions({ filter }: { filter: MagicFilter }) {
  const t = useTranslations();
  const [icsOpen, setIcsOpen] = useState(false);

  // Always a workbook (header row even for an empty result, see TimesheetPagesRest.exportAsExcel), so a
  // failure here is a real one — an access refusal — and is reported as such.
  const excel = useMutation({
    mutationFn: () => downloadTimesheetExcel(filter),
    onError: (error) =>
      toast.error(error instanceof Error ? error.message : String(error)),
  });

  return (
    <>
      <HintTooltip text={t("tooltip.export.excel")}>
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
      <HintTooltip text={t("timesheet.iCalSubscription")}>
        <Button
          type="button"
          variant="ghost"
          size="sm"
          className="gap-1.5"
          onClick={() => setIcsOpen(true)}
        >
          <HugeiconsIcon icon={Calendar03Icon} size={14} aria-hidden />
          {t("timesheet.icsExport")}
        </Button>
      </HintTooltip>
      {icsOpen && <TimesheetIcsDialog onClose={() => setIcsOpen(false)} />}
    </>
  );
}
