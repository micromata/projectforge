"use client";

import { useTranslations } from "next-intl";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { EInvoiceFindings } from "@/components/shared/e-invoice-findings";
import type { EInvoiceCheckResult } from "@/lib/rs/e-invoice-checker";
import { EInvoiceCheckerButton } from "../e-invoice-checker-button";

/**
 * The verdict of the e-invoice checker on the file that was just generated: OK, warnings or errors.
 *
 * The file is saved either way — this only says whether it holds up. The checker page shows the same
 * result in full, since the upload that produced it is the session's last one.
 */
export function EInvoiceCheckDialog({
  result,
  onClose,
}: {
  result: EInvoiceCheckResult | null;
  onClose: () => void;
}) {
  const t = useTranslations();
  return (
    <Dialog open={result != null} onOpenChange={(open) => !open && onClose()}>
      <DialogContent data-testid="e-invoice-check-dialog">
        <DialogHeader>
          <DialogTitle>{t("fibu.rechnung.eInvoice.check.title")}</DialogTitle>
        </DialogHeader>
        {result && (
          <div className="flex flex-col gap-3">
            <p className="text-sm font-medium">{result.filename}</p>
            <EInvoiceFindings data={result.invoiceData} />
            <div>
              <EInvoiceCheckerButton />
            </div>
          </div>
        )}
      </DialogContent>
    </Dialog>
  );
}
