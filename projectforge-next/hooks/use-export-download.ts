"use client";

import { useTranslations } from "next-intl";
import { useMutation } from "@tanstack/react-query";
import { toast } from "@/lib/toast";
import { RsError } from "@/lib/rs/client";

/**
 * A file download as a mutation, with the error handling every export shares.
 *
 * A 404 is no error here: the export had nothing to write — the filter matched nothing, the chart has
 * nothing to draw, or (for the cost assignments) the installation has no cost ids configured. It is
 * reported as "no records found". Everything else — including the 400 the salary cost-assignment export
 * answers with when no month is picked — carries its own message and is shown as it is.
 *
 * Generic in what `mutate` takes, so an export with a choice (the Gantt chart's format) fits as well.
 */
export function useExportDownload<TVars = void>(
  download: (vars: TVars) => Promise<void>
) {
  const t = useTranslations();
  return useMutation({
    mutationFn: download,
    onError: (error: unknown) => {
      if (error instanceof RsError && error.status === 404) {
        toast.info(t("datatable.no-records-found"));
        return;
      }
      toast.error(error instanceof Error ? error.message : String(error));
    },
  });
}
