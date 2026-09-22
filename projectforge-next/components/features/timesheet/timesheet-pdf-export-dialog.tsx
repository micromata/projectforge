"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Label } from "@/components/ui/label";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import { Spinner } from "@/components/shared/spinner";
import {
  useSubmitShortcut,
  useSubmitShortcutHint,
} from "@/hooks/use-submit-shortcut";
import {
  downloadTimesheetPdf,
  fetchTimesheetPdfExportSettings,
  TIMESHEET_PDF_EXPORT_SETTINGS_QUERY_KEY,
  type TimesheetPdfExportSettings,
} from "@/lib/rs/timesheet";
import type { MagicFilter } from "@/lib/rs/types";

export interface TimesheetPdfExportDialogProps {
  filter: MagicFilter;
  onClose: () => void;
  onError: (error: unknown) => void;
}

/** The seven optional columns, in the order the PDF prints them. The User column is always shown. */
const COLUMNS: { key: keyof TimesheetPdfExportSettings; labelKey: string }[] = [
  // `task` is also a namespace (task.status.*), so its own label lives under the `_` leaf.
  { key: "task", labelKey: "task._" },
  { key: "startTime", labelKey: "timesheet.startTime" },
  { key: "stopTime", labelKey: "timesheet.stopTime" },
  { key: "duration", labelKey: "timesheet.duration" },
  { key: "location", labelKey: "timesheet.location" },
  { key: "reference", labelKey: "timesheet.reference" },
  { key: "description", labelKey: "description" },
];

/**
 * Lets the user pick what the timesheet PDF export contains: whether the filter-settings block is printed
 * and which of the optional columns appear. The answer is stored per user by the backend
 * (`TimesheetPagesRest.exportAsPdf`), so it only has to be given once — the dialog prefills from it.
 *
 * Modeled on the order forecast export dialog. Unlike that one the export always returns a valid file even
 * for an empty result (see `TimesheetListPdfExport`), so there is no 404/empty-result contract here.
 */
export function TimesheetPdfExportDialog({
  filter,
  onClose,
  onError,
}: TimesheetPdfExportDialogProps) {
  const t = useTranslations();
  const shortcutHint = useSubmitShortcutHint();
  const queryClient = useQueryClient();
  const stored = useQuery({
    queryKey: TIMESHEET_PDF_EXPORT_SETTINGS_QUERY_KEY,
    queryFn: ({ signal }) => fetchTimesheetPdfExportSettings(signal),
  });
  // What the user changed, laid over what the backend remembers — the same pattern as the forecast dialog,
  // avoiding an effect to seed local state from the query (which could clobber the fetched answer).
  const [edits, setEdits] = useState<Partial<TimesheetPdfExportSettings>>({});
  const values: TimesheetPdfExportSettings | undefined = stored.data && {
    ...stored.data,
    ...edits,
  };
  const toggle = (key: keyof TimesheetPdfExportSettings, checked: boolean) =>
    setEdits((prev) => ({ ...prev, [key]: checked }));

  const download = useMutation({
    mutationFn: (settings: TimesheetPdfExportSettings) =>
      downloadTimesheetPdf(filter, settings),
    onSuccess: () => {
      // The export persisted the settings on the way, so the cached copy is a version behind.
      void queryClient.invalidateQueries({
        queryKey: TIMESHEET_PDF_EXPORT_SETTINGS_QUERY_KEY,
      });
      onClose();
    },
    onError,
  });

  const canSubmit = Boolean(values) && !download.isPending;
  const onKeyDown = useSubmitShortcut(
    () => values && download.mutate(values),
    canSubmit
  );

  return (
    <Dialog open onOpenChange={(open) => !open && onClose()}>
      <DialogContent onKeyDown={onKeyDown}>
        <DialogHeader>
          <DialogTitle>{t("timesheet.pdfExport.title")}</DialogTitle>
          <DialogDescription>{t("timesheet.pdfExport.info")}</DialogDescription>
        </DialogHeader>

        {!values ? (
          <div className="flex justify-center py-6">
            <Spinner />
          </div>
        ) : (
          <div className="flex flex-col gap-4">
            <div className="flex items-center gap-2">
              <Checkbox
                id="pdf-export-filter-settings"
                checked={values.showFilterSettings}
                onCheckedChange={(checked) =>
                  toggle("showFilterSettings", checked === true)
                }
              />
              <Label
                htmlFor="pdf-export-filter-settings"
                className="font-normal"
              >
                {t("timesheet.pdfExport.filterSettings")}
              </Label>
            </div>

            <div className="flex flex-col gap-2">
              <Label>{t("timesheet.pdfExport.columns")}</Label>
              {COLUMNS.map(({ key, labelKey }) => (
                <div key={key} className="flex items-center gap-2">
                  <Checkbox
                    id={`pdf-export-column-${key}`}
                    checked={values[key]}
                    onCheckedChange={(checked) => toggle(key, checked === true)}
                  />
                  <Label
                    htmlFor={`pdf-export-column-${key}`}
                    className="font-normal"
                  >
                    {t(labelKey)}
                  </Label>
                </div>
              ))}
            </div>
          </div>
        )}

        <DialogFooter>
          <Button type="button" variant="outline" onClick={onClose}>
            {t("cancel")}
          </Button>
          <HintTooltip {...shortcutHint}>
            <Button
              type="button"
              onClick={() => values && download.mutate(values)}
              disabled={!canSubmit}
            >
              {download.isPending && <Spinner className="h-4 w-4 border-2" />}
              {t("exportAsPdf")}
            </Button>
          </HintTooltip>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
