"use client";

import { useTranslations } from "next-intl";
import { useMutation, useQuery } from "@tanstack/react-query";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { FormAlert } from "@/components/shared/form-alert";
import { Spinner } from "@/components/shared/spinner";
import {
  downloadXRechnung,
  downloadZugferd,
  fetchEInvoiceValidation,
} from "@/lib/rs/invoice";
import { InvoiceExportButton } from "../invoice-export-button";

/**
 * The e-invoice of one invoice — Wicket's `EInvoiceModalDialog` (`RechnungEditForm`), reduced to what is
 * left of it here: what stands between this invoice and an e-invoice, and the two exports.
 *
 * Wicket's dialog also carried the address fields, the bank account dropdown, the invoice PDF and the
 * filtered attachment list, because none of them was on its form. Here all four are fields of the form
 * itself (the `customer` section, SellerBankAccountField, InvoicePdfField, AttachmentSection), so repeating
 * them in a dialog would be a second place to edit the same invoice.
 *
 * **Deliberately unlike Wicket**: opening this does not save. Each of Wicket's two export buttons runs
 * `getBaseDao().update(data)` and processes the PDF upload before it validates, so pressing "Export
 * XRechnung" writes the invoice — a hidden write behind a download. Here the form shows its own dirty state
 * and has its own save; what is validated and exported is the **stored** invoice, which the dialog says. It
 * is not even a free choice for ZUGFeRD: that path reads the invoice PDF and the attachments from the JCR by
 * id, so there is no unsaved state it could be built from.
 */
export function EInvoiceDialog({
  invoiceId,
  onClose,
}: {
  invoiceId: number;
  onClose: () => void;
}) {
  const t = useTranslations();
  const validation = useQuery({
    queryKey: ["outgoingInvoice", "eInvoice", invoiceId],
    queryFn: ({ signal }) => fetchEInvoiceValidation(invoiceId, signal),
    // Every open asks again: the usual reason for opening this twice is that the invoice was corrected and
    // saved in between, and a cached list of problems would still name the ones just fixed.
    staleTime: 0,
    refetchOnMount: "always",
  });

  const download = useMutation({
    mutationFn: (kind: "xrechnung" | "zugferd") =>
      kind === "xrechnung"
        ? downloadXRechnung(invoiceId)
        : downloadZugferd(invoiceId),
    // The backend's own answer — for a refused export the list of what is missing, which
    // `downloadFile` carries out of the response body (see lib/rs/download.ts).
    onError: (error: unknown) =>
      toast.error(error instanceof Error ? error.message : String(error)),
  });

  const errors = validation.data?.errors ?? [];
  const configured = validation.data?.configured ?? false;
  const exportable = validation.isSuccess && configured && errors.length === 0;

  return (
    <Dialog open onOpenChange={(open) => !open && onClose()}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>{t("fibu.konto.eInvoice")}</DialogTitle>
          <DialogDescription>
            {t("invoice.eInvoice.savedOnlyHint")}
          </DialogDescription>
        </DialogHeader>

        {validation.isPending ? (
          <div className="flex justify-center py-6">
            <Spinner />
          </div>
        ) : (
          <div className="flex flex-col gap-3">
            {!configured && (
              <FormAlert tone="error">
                {t("invoice.eInvoice.notConfigured")}
              </FormAlert>
            )}
            {errors.length > 0 && (
              <FormAlert tone="error">
                <p className="font-medium">
                  {t("fibu.rechnung.eInvoice.validationErrors")}
                </p>
                {/* The backend's untranslated English prose, rendered as it arrives:
                    `EInvoiceExportService.validate` builds sentences, not keys. A known debt of this
                    migration, shared with Wicket's own dialog — see MIGRATION.md. */}
                <ul className="mt-1 list-disc pl-5">
                  {errors.map((error) => (
                    <li key={error}>{error}</li>
                  ))}
                </ul>
              </FormAlert>
            )}
            {exportable && (
              <p className="text-sm text-muted-foreground">
                {t("fibu.rechnung.invoicePdf.hint")}
              </p>
            )}
          </div>
        )}

        <DialogFooter>
          <Button type="button" variant="outline" onClick={onClose}>
            {t("cancel")}
          </Button>
          {/* Both disabled while anything is missing, as Wicket refuses both then — and the export would
              refuse them anyway (`OutgoingInvoiceEntityRest.exportEInvoice` answers the same list). The
              tooltip of a disabled one names the reason, which is above it in full. */}
          {EXPORTS.map(({ kind, labelKey }) => (
            <InvoiceExportButton
              key={kind}
              label={t(labelKey)}
              tooltip={
                exportable
                  ? t(labelKey)
                  : t("fibu.rechnung.eInvoice.validationErrors")
              }
              disabled={!exportable}
              isPending={download.isPending && download.variables === kind}
              onClick={() => download.mutate(kind)}
              // Not the ghost of a toolbar: in a dialog footer these are what the dialog is for.
              variant="default"
              size="default"
            />
          ))}
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}

/** The two e-invoice formats, in Wicket's order: the XML alone, then the PDF carrying it. */
const EXPORTS = [
  { kind: "xrechnung", labelKey: "fibu.rechnung.exportEInvoice" },
  { kind: "zugferd", labelKey: "fibu.rechnung.exportZUGFeRD" },
] as const;
