"use client";

import { useRef } from "react";
import { useTranslations } from "next-intl";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "@/lib/toast";
import { HugeiconsIcon } from "@hugeicons/react";
import { CloudUploadIcon, Delete02Icon } from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import { Spinner } from "@/components/shared/spinner";
import { AttachmentDropZone } from "@/components/shared/attachments/attachment-drop-zone";
import { attachmentsQueryKey } from "@/hooks/use-attachments";
import { leafKeyOf } from "@/lib/leaf-key";
import { attachmentDownloadUrl } from "@/lib/rs/attachments";
import {
  deleteInvoicePdf,
  fetchInvoicePdfInfo,
  invoicePdfQueryKey,
  uploadInvoicePdf,
  type InvoicePdfState,
} from "@/lib/rs/invoice-pdf";

/**
 * The invoice PDF of one invoice — Wicket's `fibu.rechnung.invoicePdf` fieldset
 * (`RechnungEditForm`, the e-invoice dialog).
 *
 * One file with a role rather than an attachment among others: the ZUGFeRD export embeds its XML into this
 * very PDF and converts the Word template only where none is stored, which is what the hint says. So there
 * is exactly one, and uploading a second replaces the first (see lib/rs/invoice-pdf.ts). Being an attachment,
 * it also has a row in the attachment list below — which is where a delete on either side has to reach the
 * other one, since the two are separate queries of one file (see AttachmentSection).
 *
 * Takes a file the same three ways the attachment section takes one: the button, the file dialog, and a drop
 * anywhere on the field.
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
    queryKey: invoicePdfQueryKey(invoiceId),
    queryFn: ({ signal }) => fetchInvoicePdfInfo(invoiceId!, signal),
    enabled,
  });

  /**
   * Writes the answered state into the cache and drops the attachment list with it: both writes go through
   * `EInvoiceExportService.updateAttachmentsCounter`, so the counter and the names the attachment section
   * and the list column show have just changed.
   */
  const applyState = (state: InvoicePdfState) => {
    qc.setQueryData(invoicePdfQueryKey(invoiceId), state);
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

  /**
   * Takes a file dropped anywhere on this field — the same gesture the attachment section offers, which
   * is where one expects to be able to drop an invoice PDF as well.
   *
   * Of several dropped files none is taken: this field holds exactly one PDF, so a drop of two says the
   * user meant something it cannot do, and picking one of them silently would be a guess. Whether the one
   * file is a PDF at all stays the backend's answer (see uploadInvoicePdf).
   */
  const onDropped = (files: File[]) => {
    if (files.length > 1) {
      toast.error(t("file.upload.error.tooManyFiles"));
      return;
    }
    upload.mutate(files[0]);
  };

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
    // The whole field takes a drop, as the attachment section does: no drop box of its own, the dashed
    // area only appears while something is dragged over it (see AttachmentDropZone).
    <AttachmentDropZone onFiles={onDropped} disabled={busy}>
      <div className="flex flex-col gap-2">
        <div className="flex flex-wrap items-center gap-2">
          {info.isLoading ? (
            <Spinner className="h-4 w-4 border-2" />
          ) : pdf?.fileId ? (
            // The name is the download, as it is on an attachment row: a plain link, because the answer is
            // the file itself and the browser has to handle it (see attachmentDownloadUrl).
            <a
              href={attachmentDownloadUrl({
                entity: "outgoingInvoice",
                id: invoiceId!,
                fileId: pdf.fileId,
              })}
              // Named after the field, not after the file: the attachment list below shows this very file
              // as well, and two links of the same name would answer to one another (see AttachmentRow).
              aria-label={`${t("download._")}: ${title}`}
              className="text-sm hover:underline"
            >
              {`${pdf.name} (${pdf.sizeHumanReadable})`}
            </a>
          ) : (
            // No `fileId`: nothing stored — or a node this instance cannot serve, and then a link would
            // only lead to a 404.
            <span className="text-sm">
              {pdf
                ? `${pdf.name} (${pdf.sizeHumanReadable})`
                : t("nothingFound")}
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
              // Named after the field, like the download link above and for the same reason: the row this
              // file has in the attachment list carries a delete named after the file itself.
              aria-label={`${t("delete")}: ${title}`}
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
    </AttachmentDropZone>
  );
}
