"use client";

import { useTranslations } from "next-intl";
import { useMutation } from "@tanstack/react-query";
import { toast } from "sonner";
import { HugeiconsIcon } from "@hugeicons/react";
import { Download04Icon } from "@hugeicons/core-free-icons";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import { Spinner } from "@/components/shared/spinner";
import {
  downloadMassUpdateProtocol,
  type MassUpdateResult,
} from "@/lib/rs/multi-select";

/**
 * What a run did: the counters, the fields it touched, whatever failed, and the Excel protocol.
 *
 * The protocol is offered rather than pushed, although the backend also puts it into the user's data
 * transfer box: the box is where it is found tomorrow, this is where it is found now.
 */
export function MassUpdateResultPanel({
  result,
}: {
  result: MassUpdateResult;
}) {
  const t = useTranslations();
  const download = useMutation({
    mutationFn: (url: string) => downloadMassUpdateProtocol(url),
    onError: (error) =>
      toast.error(error instanceof Error ? error.message : String(error)),
  });

  return (
    <div className="space-y-3">
      <Alert variant={result.errorCounter > 0 ? "destructive" : "default"}>
        <AlertTitle>{result.resultMessage}</AlertTitle>
        {result.changedFields.length > 0 && (
          <AlertDescription>
            {`${t("massUpdate.fields.changed")}: ${result.changedFields.join(", ")}`}
          </AlertDescription>
        )}
      </Alert>

      {result.errors.length > 0 && (
        <div className="space-y-1">
          <h2 className="text-xs font-semibold uppercase tracking-wide text-muted-foreground">
            {t("massUpdate.error.table.title")}
          </h2>
          {/* A plain list, not a DataTable: two columns of a handful of rows, with no sorting, no
              filtering and no column state to remember. */}
          <ul className="space-y-1 text-xs">
            {result.errors.map((error, index) => (
              <li key={`${error.identifier}-${index}`} className="flex gap-2">
                <span className="font-medium">{error.identifier}</span>
                <span className="text-muted-foreground">{error.message}</span>
              </li>
            ))}
          </ul>
        </div>
      )}

      {result.downloadUrl && (
        <Button
          type="button"
          variant="outline"
          size="sm"
          className="gap-1.5"
          disabled={download.isPending}
          onClick={() => download.mutate(result.downloadUrl!)}
        >
          {download.isPending ? (
            <Spinner className="h-3.5 w-3.5 border-2" />
          ) : (
            <HugeiconsIcon icon={Download04Icon} size={14} aria-hidden />
          )}
          {t("massUpdate.excel.download")}
        </Button>
      )}
    </div>
  );
}
