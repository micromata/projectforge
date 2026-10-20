"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Progress } from "@/components/ui/progress";
import { AttachmentDropArea } from "@/components/shared/attachments/attachment-drop-area";
import { FormAlert } from "@/components/shared/form-alert";
import { MarkdownText } from "@/components/shared/markdown-text";
import { PageShell } from "@/components/shared/page-shell";
import { PageTitleRow } from "@/components/shared/page-title-row";
import {
  E_INVOICE_CHECK_QUERY_KEY,
  fetchLastCheck,
  uploadEInvoice,
} from "@/lib/rs/e-invoice-checker";
import { EInvoiceCheckResultView } from "./e-invoice-check-result";

/**
 * The e-invoice checker (`/next/eInvoiceChecker`, formerly the dynamic React page `react/eInvoiceChecker/dynamic`):
 * reads an uploaded ZUGFeRD PDF or XRechnung XML back and shows what is in it — validity, invoice data, seller,
 * buyer, amounts, line items and the embedded attachments.
 *
 * The drop area stays above the result, so the next file is checked without leaving it. The last result is
 * the session's (`EInvoiceCheckerRest`), so a reload shows it again.
 *
 * FIBU/ORGA groups or order-book users; the endpoints check it and answer everybody else with 403.
 */
export function EInvoiceCheckerPage() {
  const t = useTranslations();
  const queryClient = useQueryClient();
  const [progress, setProgress] = useState<number | null>(null);
  const query = useQuery({
    queryKey: E_INVOICE_CHECK_QUERY_KEY,
    queryFn: ({ signal }) => fetchLastCheck(signal),
  });
  const upload = useMutation({
    mutationFn: (file: File) => {
      setProgress(0);
      return uploadEInvoice(file, {
        onProgress: (p) => setProgress(p.percent ?? 0),
      });
    },
    onSuccess: (result) =>
      queryClient.setQueryData(E_INVOICE_CHECK_QUERY_KEY, result),
    onSettled: () => setProgress(null),
  });
  const uploading = upload.isPending;
  // A refused file leaves the previous result in place, as the error explains the file just dropped.
  const result = query.data;

  return (
    <PageShell>
      <PageTitleRow
        category={t("menu.fibu._")}
        title={t("fibu.eInvoiceChecker.title")}
      />
      <div className="flex flex-col gap-4 px-4 pb-6 pt-2">
        <MarkdownText
          text={t("fibu.eInvoiceChecker.description")}
          className="text-sm text-muted-foreground"
        />
        {query.isError && (
          <FormAlert tone="error">{t("access.exception.noAccess")}</FormAlert>
        )}
        {upload.isError && (
          <FormAlert tone="error">{upload.error.message}</FormAlert>
        )}
        {!query.isError && (
          <div className="flex flex-col gap-3">
            <AttachmentDropArea
              onFiles={(files) => files[0] && upload.mutate(files[0])}
              accept=".pdf,.xml,application/pdf,application/xml,text/xml"
              multiple={false}
              disabled={uploading}
              label={t("file.upload.dropArea")}
            />
            {progress !== null && (
              <div className="flex items-center gap-2">
                <Progress value={progress} className="flex-1" />
                <span className="w-12 text-right text-xs tabular-nums text-muted-foreground">
                  {progress} %
                </span>
              </div>
            )}
          </div>
        )}
        {result && <EInvoiceCheckResultView result={result} />}
      </div>
    </PageShell>
  );
}
