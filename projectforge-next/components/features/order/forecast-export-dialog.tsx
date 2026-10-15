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
import { DateInput } from "@/components/shared/date-input";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import { Spinner } from "@/components/shared/spinner";
import {
  useSubmitShortcut,
  useSubmitShortcutHint,
} from "@/hooks/use-submit-shortcut";
import {
  downloadOrderForecast,
  fetchForecastExportSettings,
  FORECAST_SETTINGS_QUERY_KEY,
  type ForecastExportSettings,
} from "@/lib/rs/order";
import { RsError } from "@/lib/rs/client";
import type { MagicFilter } from "@/lib/rs/types";

export interface ForecastExportDialogProps {
  filter: MagicFilter;
  onClose: () => void;
  /** Called when the filter matched no order, so the caller can say so where it says it otherwise. */
  onEmptyResult: () => void;
  onError: (error: unknown) => void;
}

/**
 * Asks for the two things the forecast export cannot derive: the month it starts with and the budget
 * scenario.
 *
 * Wicket takes the start month from the period-of-performance filter and falls back to January of the
 * current year without saying so, which makes the most important property of the sheet a side effect of a
 * list filter. Here it is asked for, and the answer is stored per user by the backend
 * (`OrderEntityRest.exportForecast`), so it only has to be given once.
 */
export function ForecastExportDialog({
  filter,
  onClose,
  onEmptyResult,
  onError,
}: ForecastExportDialogProps) {
  const t = useTranslations();
  const shortcutHint = useSubmitShortcutHint();
  const queryClient = useQueryClient();
  const stored = useQuery({
    queryKey: FORECAST_SETTINGS_QUERY_KEY,
    queryFn: ({ signal }) => fetchForecastExportSettings(signal),
  });
  // What the user changed, laid over what the backend remembers — rather than a copy seeded from the
  // query, which would need an effect to catch the answer and could overwrite it with a default.
  const [edits, setEdits] = useState<Partial<ForecastExportSettings>>({});
  const values: ForecastExportSettings | undefined = stored.data && {
    ...stored.data,
    ...edits,
  };
  const setValues = (next: ForecastExportSettings) => setEdits(next);

  const download = useMutation({
    mutationFn: (settings: ForecastExportSettings) =>
      downloadOrderForecast(filter, settings),
    onSuccess: () => {
      // The export persisted the settings on the way, so the cached copy is a version behind.
      void queryClient.invalidateQueries({
        queryKey: FORECAST_SETTINGS_QUERY_KEY,
      });
      onClose();
    },
    onError: (error) => {
      if (error instanceof RsError && error.status === 404) {
        onEmptyResult();
        onClose();
        return;
      }
      onError(error);
    },
  });

  // Return exports — except inside the date field, which reads Return itself to commit what was
  // typed (see DateInput, and isSubmitShortcut on `defaultPrevented`).
  const canSubmit = Boolean(values?.startDate) && !download.isPending;
  const onKeyDown = useSubmitShortcut(
    () => values && download.mutate(values),
    canSubmit
  );

  return (
    <Dialog open onOpenChange={(open) => !open && onClose()}>
      {/* Radix focuses the first field when the dialog opens, and focusing [DateInput] opens its
          calendar — over the very date it is showing. The dialog opens with nothing focused instead;
          the remembered answer is usually the one wanted, and Tab reaches the field. */}
      <DialogContent
        onOpenAutoFocus={(event) => event.preventDefault()}
        onKeyDown={onKeyDown}
      >
        <DialogHeader>
          <DialogTitle>{t("fibu.auftrag.forecastExportAsXls._")}</DialogTitle>
          <DialogDescription>
            {t("fibu.auftrag.forecastExport.tooltip")}
          </DialogDescription>
        </DialogHeader>

        {!values ? (
          <div className="flex justify-center py-6">
            <Spinner />
          </div>
        ) : (
          <div className="flex flex-col gap-4">
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="forecast-export-start-date">
                {t("fibu.auftrag.forecastExport.startDate._")}
              </Label>
              {/* No autoFocus: focusing the field opens the date picker, which is in the way when the
                  remembered date is the one wanted anyway. */}
              <DateInput
                id="forecast-export-start-date"
                value={values.startDate}
                onChange={(startDate) => setValues({ ...values, startDate })}
              />
              <p className="text-xs text-muted-foreground">
                {t("fibu.auftrag.forecastExport.startDate.info")}
              </p>
            </div>

            <div className="flex flex-col gap-1.5">
              <div className="flex items-center gap-2">
                <Checkbox
                  id="forecast-export-distribute-budget"
                  checked={values.distributeUnusedBudget}
                  onCheckedChange={(checked) =>
                    setValues({
                      ...values,
                      distributeUnusedBudget: checked === true,
                    })
                  }
                />
                <Label
                  htmlFor="forecast-export-distribute-budget"
                  className="font-normal"
                >
                  {t("fibu.auftrag.forecast.analysis.variants.true.label")}
                </Label>
              </div>
              <p className="text-xs text-muted-foreground">
                {t("fibu.auftrag.forecast.analysis.variants.true._")}
              </p>
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
              {t("exportAsXls")}
            </Button>
          </HintTooltip>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
