"use client";

import { useRef } from "react";
import { useTranslations } from "next-intl";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "@/lib/toast";
import { HugeiconsIcon } from "@hugeicons/react";
import { CloudUploadIcon, Delete02Icon } from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import { Spinner } from "@/components/shared/spinner";
import { attachmentsQueryKey } from "@/hooks/use-attachments";
import { leafKeyOf } from "@/lib/leaf-key";
import {
  deleteInvoicePdf,
  fetchInvoicePdfInfo,
  uploadInvoicePdf,
  type InvoicePdfState,
} from "@/lib/rs/invoice-pdf";

/**
 * The invoice PDF of one invoice — Wicket's `fibu.rechnung.invoicePdf` fieldset
 * (`RechnungEditForm`, the e-invoice dialog).
 *
 * One file with a role rather than an attachment among others: the ZUGFeRD export embeds its XML into this
 * very PDF and converts the Word template only where none is stored, which is what the hint says. So there
 * is exactly one, and uploading a second replaces the first (see lib/rs/invoice-pdf.ts). It is filtered out
 * of the attachment list below, so no file appears twice on this page (see AttachmentSection).
 *
 * Offered for a stored invoice only, as Wicket offers it: the file hangs off the invoice's JCR node, which
 * needs the persisted id — the same reason attachments say so in their own words.
 */
export function InvoicePdfField({ invoiceId }: { invoiceId: number | null }) {
  const t = useTranslations();
  const qc = useQueryClient();
  const inputRef = useRef<HTMLInputElement>(null);
  const enabled = invoiceId != null && invoiceId > 0;

  const info = useQuery({
    queryKey: ["outgoingInvoice", "invoicePdf", invoiceId],
    queryFn: ({ signal }) => fetchInvoicePdfInfo(invoiceId!, signal),
    enabled,
  });

  /**
   * Writes the answered state into the cache and drops the attachment list with it: both writes go through
   * `EInvoiceExportService.updateAttachmentsCounter`, so the counter and the names the attachment section
   * and the list column show have just changed.
   */
  const applyState = (state: InvoicePdfState) => {
    qc.setQueryData(["outgoingInvoice", "invoicePdf", invoiceId], state);
    qc.invalidateQueries({
      queryKey: attachmentsQueryKey("outgoingInvoice", invoiceId),
    });
  };
  const onError = (error: unknown) =>
    // The backend's own text for a refusal (not a PDF, too large), which lib/rs/invoice-pdf.ts carries.
    toast.error(error instanceof Error ? error.message : String(error));

  const upload = useMutation({
    mutationFn: (file: File) => uploadInvoicePdf(invoiceId!, file),
    onSuccess: applyState,
    onError,
  });
  const remove = useMutation({
    mutationFn: () => deleteInvoicePdf(invoiceId!),
    onSuccess: applyState,
    onError,
  });

  if (!enabled) {
    return (
      <p className="text-sm text-muted-foreground">
        {t("attachment.onlyAvailableAfterSave")}
      </p>
    );
  }

  const pdf = info.data?.pdf;
  const busy = upload.isPending || remove.isPending;
  // `_`, since the key has a `hint` subkey and so becomes a namespace.
  const title = t(leafKeyOf("fibu.rechnung.invoicePdf", t.has));

  return (
    <div className="flex flex-col gap-2">
      <div className="flex flex-wrap items-center gap-2">
        {info.isLoading ? (
          <Spinner className="h-4 w-4 border-2" />
        ) : (
          <span className="text-sm">
            {pdf ? `${pdf.name} (${pdf.sizeHumanReadable})` : t("nothingFound")}
          </span>
        )}
        <Button
          type="button"
          variant="outline"
          size="sm"
          className="h-7 gap-1.5 text-[11px]"
          disabled={busy}
          onClick={() => inputRef.current?.click()}
        >
          {upload.isPending ? (
            <Spinner className="h-3 w-3 border-2" />
          ) : (
            <HugeiconsIcon icon={CloudUploadIcon} size={13} />
          )}
          {title}
        </Button>
        {pdf && (
          <Button
            type="button"
            variant="ghost"
            size="sm"
            className="h-7 gap-1.5 text-[11px] text-destructive"
            disabled={busy}
            // Named after the file, as the attachment rows are: the section carries a second delete.
            aria-label={`${t("delete")}: ${pdf.name}`}
            onClick={() => remove.mutate()}
          >
            {remove.isPending ? (
              <Spinner className="h-3 w-3 border-2" />
            ) : (
              <HugeiconsIcon icon={Delete02Icon} size={13} />
            )}
            {t("delete")}
          </Button>
        )}
      </div>
      <p className="text-xs text-muted-foreground">
        {t("fibu.rechnung.invoicePdf.hint")}
      </p>
      <input
        ref={inputRef}
        type="file"
        accept=".pdf"
        // sr-only rather than hidden, as AttachmentAddButton explains: a hidden input is not focusable.
        className="sr-only"
        // The attachment section of the same page has a file input of its own, whose name is the bare
        // `file.upload.choose` — so this one says which of the two it is.
        aria-label={`${t("file.upload.choose")}: ${title}`}
        onChange={(e) => {
          const file = e.target.files?.[0];
          if (file) upload.mutate(file);
          // Cleared so choosing the same file twice fires change again — the second attempt is what
          // surfaces a refusal the user has meanwhile fixed.
          e.target.value = "";
        }}
      />
    </div>
  );
}
