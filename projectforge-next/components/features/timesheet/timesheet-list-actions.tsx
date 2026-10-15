"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { Calendar03Icon, Pdf01Icon } from "@hugeicons/core-free-icons";
import { CalendarSubscriptionDialog } from "@/components/shared/calendar-subscription/calendar-subscription-dialog";
import {
  ExcelExportMenuItem,
  ExportMenu,
  ExportMenuItem,
} from "@/components/shared/export-menu";
import { useExportDownload } from "@/hooks/use-export-download";
import { toast } from "@/lib/toast";
import { downloadTimesheetExcel } from "@/lib/rs/timesheet";
import type { MagicFilter } from "@/lib/rs/types";
import { TimesheetPdfExportDialog } from "./timesheet-pdf-export-dialog";

/**
 * The three exports of the time sheet list in its export menu, as the legacy list offers them in its
 * content menu: the filtered list as Excel or PDF, and the ics subscription url.
 *
 * The Excel and PDF exports act on the filter the list is showing, which is why they live in the toolbar
 * and are handed that filter (see PageDef.listActions). The ics url is the user's own and opens a dialog
 * (see CalendarSubscriptionDialog).
 */
export function TimesheetListActions({ filter }: { filter: MagicFilter }) {
  const t = useTranslations();
  const [icsOpen, setIcsOpen] = useState(false);
  const [pdfOpen, setPdfOpen] = useState(false);
  const excel = useExportDownload(() => downloadTimesheetExcel(filter));

  return (
    <>
      <ExportMenu isPending={excel.isPending}>
        {/* Always answers with a valid file (a header row even for an empty result, see
            TimesheetEntityRest), so a failure here is a real one — an access refusal. */}
        <ExcelExportMenuItem onSelect={() => excel.mutate()} />
        <ExportMenuItem
          icon={Pdf01Icon}
          label={t("exportAsPdf")}
          description={t("tooltip.export.pdf")}
          onSelect={() => setPdfOpen(true)}
        />
        <ExportMenuItem
          icon={Calendar03Icon}
          label={t("timesheet.icsExport")}
          description={t("timesheet.iCalSubscription")}
          onSelect={() => setIcsOpen(true)}
        />
      </ExportMenu>
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
