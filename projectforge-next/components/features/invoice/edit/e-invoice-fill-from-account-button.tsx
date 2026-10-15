"use client";

import { useTranslations } from "next-intl";
import { useMutation } from "@tanstack/react-query";
import { useStore } from "@tanstack/react-form";
import { HugeiconsIcon } from "@hugeicons/react";
import { UserAccountIcon } from "@hugeicons/core-free-icons";
import { toast } from "@/lib/toast";
import { Button } from "@/components/ui/button";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import { Spinner } from "@/components/shared/spinner";
import { useEntityEditForm } from "@/components/shared/form/form-context";
import { EInvoiceCheckerButton } from "../e-invoice-checker-button";
import { useFillEInvoiceFromAccount } from "./use-fill-e-invoice-from-account";

/**
 * The heading actions of the e-invoice section: the fill from the account, which is about the fields of the
 * section, and the checker, which is about what an export of them yields — `SectionDef.headerActions` takes
 * one component, so the two come as one.
 */
export function EInvoiceSectionHeaderActions() {
  return (
    <div className="flex items-center gap-1">
      <EInvoiceFillFromAccountButton />
      <EInvoiceCheckerButton />
    </div>
  );
}

/**
 * "Fill in from account": the empty e-invoice fields of this invoice from its account — for an invoice that
 * names its customer already, so the fill that picking one does never comes (an old invoice from before the
 * e-invoice; a clone of one is filled by the backend, see `OutgoingInvoiceEntityRest.prepareClone`).
 *
 * Only the empty fields, as every fill of this form: what is filled in may differ from the account on
 * purpose. The toast says how many were filled, since the fields may be a screen away from the heading.
 *
 * Disabled while the invoice names neither a customer nor an account — there is nothing to fill from.
 */
export function EInvoiceFillFromAccountButton() {
  const t = useTranslations();
  const form = useEntityEditForm();
  const fill = useFillEInvoiceFromAccount();
  const hasAccountSource = useStore(form.store, (s: unknown) => {
    const values = (s as { values: Record<string, unknown> }).values;
    return (
      (values.customer as { id?: number } | null)?.id != null ||
      (values.konto as { id?: number } | null)?.id != null
    );
  });

  const run = useMutation({
    mutationFn: fill,
    onSuccess: (count) =>
      count > 0
        ? toast.success(
            t("fibu.rechnung.eInvoice.fillFromAccountDone", { arg0: count })
          )
        : toast.info(t("fibu.rechnung.eInvoice.fillFromAccountNothing")),
    onError: (error: unknown) =>
      toast.error(error instanceof Error ? error.message : String(error)),
  });

  return (
    <HintTooltip text={t("fibu.rechnung.eInvoice.fillFromAccountHint")}>
      <Button
        type="button"
        variant="ghost"
        size="sm"
        className="gap-1.5"
        disabled={!hasAccountSource || run.isPending}
        onClick={() => run.mutate()}
      >
        {run.isPending ? (
          <Spinner className="h-3.5 w-3.5 border-2" />
        ) : (
          <HugeiconsIcon icon={UserAccountIcon} size={14} aria-hidden />
        )}
        {t("fibu.rechnung.eInvoice.fillFromAccount")}
      </Button>
    </HintTooltip>
  );
}
