"use client";

import { useStore } from "@tanstack/react-form";
import { useTranslations } from "next-intl";
import { useMutation } from "@tanstack/react-query";
import { toast } from "@/lib/toast";
import { useEntityEditForm } from "@/components/shared/form/form-context";
import {
  downloadXRechnung,
  downloadZugferd,
  type EInvoiceValidation,
} from "@/lib/rs/invoice";
import type { SubmitMeta } from "@/lib/rs/submit-meta";
import { ExportButton } from "@/components/shared/export-button";

/**
 * The two buttons of the e-invoice section: save the form, then build the e-invoice from what was saved —
 * Wicket's `fibu.rechnung.eInvoice.saveAndOpen` and its two export buttons, in one step each.
 *
 * **Why each button saves.** Everything on the e-invoice path works on the *stored* invoice (the ZUGFeRD
 * export reads the PDF and the attachments from the JCR by id, so it cannot do otherwise), while the form
 * above may have unsaved changes. Exporting without saving would hand out a document that does not match
 * the form the user is looking at, and a separate save button next to it would make that the user's problem.
 * Wicket writes as well before it exports (`getBaseDao().update(data)`); the difference is that here the
 * button says so.
 *
 * Never disabled by the checklist: saving is what may fix what the checklist names. Each step is a gate
 * instead — a refused save leaves its errors at the fields, and an invoice the export refuses leaves them in
 * the checklist above (see `OutgoingInvoiceEntityRest.saveAndCheckEInvoice` and lib/rs/submit-meta.ts, the
 * page stays put for a declared action). Pressing one of these is also what brings that checklist out in the
 * first place: `revalidate` is the only thing that asks for it (see EInvoiceSection).
 */
export function EInvoiceActions({
  invoiceId,
  revalidate,
}: {
  invoiceId: number;
  /**
   * Re-reads what still stands between the stored invoice and an e-invoice of it — the section's own query,
   * so the checklist above shows the same answer this decides on.
   */
  revalidate: () => Promise<EInvoiceValidation | undefined>;
}) {
  const t = useTranslations();
  const form = useEntityEditForm();
  const isSubmitting = useStore(
    form.store,
    (s: unknown) => (s as { isSubmitting: boolean }).isSubmitting
  );

  const run = useMutation({
    mutationFn: async (kind: "xrechnung" | "zugferd") => {
      let written = false;
      // The meta is typed here rather than inferred: `form` is the shared layer's untyped EntityForm (see
      // form-context.tsx), so nothing would check the shape of what is passed to the submit.
      const meta: SubmitMeta<"saveAndCheckEInvoice"> = {
        action: "saveAndCheckEInvoice",
        onWritten: (result) => {
          written = result.kind === "ok";
        },
      };
      await form.handleSubmit(meta);
      // Nothing was written — the form's own validation stopped the submit, or the server refused it. Either
      // way its reasons are at the fields it named (or in a toast for the ones no field shows), and
      // exporting the state before them would answer a different question than the button asked.
      if (!written) return;
      const validation = await revalidate();
      if (validation && validation.errors.length > 0) {
        // The checklist above now lists them in full — the toast is only there because the button that was
        // pressed may be a screen away from it.
        toast.error(t("fibu.rechnung.eInvoice.validationErrors"));
        return;
      }
      await (kind === "xrechnung"
        ? downloadXRechnung(invoiceId)
        : downloadZugferd(invoiceId));
    },
    // The backend's own answer — for a refused export the list of what is missing, which `downloadFile`
    // carries out of the response body (see lib/rs/download.ts).
    onError: (error: unknown) =>
      toast.error(error instanceof Error ? error.message : String(error)),
  });

  return (
    <div className="flex flex-wrap items-center gap-2">
      {EXPORTS.map(({ kind, labelKey }) => (
        <ExportButton
          key={kind}
          label={t(labelKey)}
          // No tooltip: the label names both halves of what the button does, and a note about the saved
          // state would be about a case these buttons no longer have.
          isPending={
            (run.isPending && run.variables === kind) ||
            // Every submit of this form, not only this button's: while one runs, the other would post the
            // same values a second time.
            isSubmitting
          }
          onClick={() => run.mutate(kind)}
        />
      ))}
    </div>
  );
}

/** The two e-invoice formats, in Wicket's order: the XML alone, then the PDF carrying it. */
const EXPORTS = [
  { kind: "xrechnung", labelKey: "fibu.rechnung.eInvoice.saveAndXRechnung" },
  { kind: "zugferd", labelKey: "fibu.rechnung.eInvoice.saveAndZugferd" },
] as const;
