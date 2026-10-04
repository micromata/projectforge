"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { Calendar03Icon, Pdf01Icon } from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import { CalendarSubscriptionDialog } from "@/components/shared/calendar-subscription/calendar-subscription-dialog";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import { ExcelExportButton } from "@/components/shared/excel-export-button";
import { toast } from "@/lib/toast";
import { downloadTimesheetExcel } from "@/lib/rs/timesheet";
import type { MagicFilter } from "@/lib/rs/types";
import { TimesheetPdfExportDialog } from "./timesheet-pdf-export-dialog";

/**
 * The three exports of the time sheet list the legacy list offers in its content menu: the filtered list
 * as Excel or PDF, and the ics subscription url.
 *
 * The Excel and PDF exports act on the filter the list is showing, which is why they live in the toolbar
 * and are handed that filter (see PageDef.listActions). The ics url is the user's own and opens a dialog
 * (see CalendarSubscriptionDialog).
 */
export function TimesheetListActions({ filter }: { filter: MagicFilter }) {
  const t = useTranslations();
  const [icsOpen, setIcsOpen] = useState(false);
  const [pdfOpen, setPdfOpen] = useState(false);

  return (
    <>
      {/* Always answers with a valid file (a header row even for an empty result, see
          TimesheetEntityRest), so a failure here is a real one — an access refusal. */}
      <ExcelExportButton download={() => downloadTimesheetExcel(filter)} />
      <HintTooltip text={t("tooltip.export.pdf")}>
        <Button
          type="button"
          variant="ghost"
          size="sm"
          className="gap-1.5"
          onClick={() => setPdfOpen(true)}
        >
          <HugeiconsIcon icon={Pdf01Icon} size={14} aria-hidden />
          {t("exportAsPdf")}
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
      {icsOpen && (
        <CalendarSubscriptionDialog
          type="TIMESHEETS"
          description={t("timesheet.iCalSubscription")}
          onClose={() => setIcsOpen(false)}
        />
      )}
      {pdfOpen && (
        <TimesheetPdfExportDialog
          filter={filter}
          onClose={() => setPdfOpen(false)}
          onError={(error) =>
            toast.error(error instanceof Error ? error.message : String(error))
          }
        />
      )}
    </>
  );
}
