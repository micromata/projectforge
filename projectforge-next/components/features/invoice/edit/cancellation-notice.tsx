"use client";

import { useStore } from "@tanstack/react-form";
import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import {
  AlertCircleIcon,
  InformationCircleIcon,
} from "@hugeicons/core-free-icons";
import { Alert, AlertDescription } from "@/components/ui/alert";
import {
  useEntityData,
  useEntityEditForm,
} from "@/components/shared/form/form-context";
import { cn } from "@/lib/utils";
import type { InvoiceValues } from "../invoice-schema";
import type { InvoiceDetail } from "../types";

/**
 * Atop the form, the two ends of a cancellation each say what it means:
 * - A cancellation: how it works — negated positions, the number "<original>-S", the cancelled invoice
 *   set to STORNIERT on save (`RechnungDao`), restored when the cancellation is deleted. While it is new
 *   — prepared by "Create cancellation" or typed by hand — it first says, in the warning colour, that
 *   nothing is cancelled yet: only saving it does, leaving the page without saving changes nothing.
 * - An invoice cancelled by one: that its status comes from the cancellation, and how to undo it.
 *
 * Tinted rather than the plain card alert, so it is not read past as one more field.
 */
export function CancellationNotice({ className }: { className?: string }) {
  const t = useTranslations();
  const form = useEntityEditForm();
  const typ = useStore(
    form.store,
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    (state: any) => (state.values as InvoiceValues).typ
  );
  const data = useEntityData<InvoiceDetail>();
  // A new entry has data too (the `fetchNew` preset), so it is told apart by its id.
  const isNew = data?.id == null;
  const cancelledBy = data?.cancellationInvoice;

  if (typ === "CANCELLATION") {
    return (
      <div className={cn("grid gap-2", className)}>
        {isNew && (
          <Notice tone="warning">
            {t("fibu.rechnung.cancellation.hint.unsaved")}
          </Notice>
        )}
        <Notice tone="info">
          {t("fibu.rechnung.cancellation.hint.system")}
        </Notice>
      </div>
    );
  }
  if (cancelledBy) {
    return (
      <Notice tone="info" className={className}>
        {t("fibu.rechnung.cancellation.hint.original", {
          arg0: cancelledBy.belegNummer ?? String(cancelledBy.id ?? ""),
        })}
      </Notice>
    );
  }
  return null;
}

const TONE = {
  info: {
    icon: InformationCircleIcon,
    className: "border-status-info-border bg-status-info-bg text-status-info",
  },
  warning: {
    icon: AlertCircleIcon,
    className: "border-warning/40 bg-warning/10 text-warning",
  },
} as const;

/** One tinted alert: the icon and border in the tone's colour, the text in the foreground for contrast. */
function Notice({
  tone,
  className,
  children,
}: {
  tone: keyof typeof TONE;
  className?: string;
  children: string;
}) {
  const { icon, className: toneClass } = TONE[tone];
  return (
    <Alert className={cn("px-3 py-2", toneClass, className)}>
      <HugeiconsIcon icon={icon} size={16} aria-hidden />
      <AlertDescription
        className={cn(
          "text-sm text-foreground",
          tone === "warning" && "font-medium"
        )}
      >
        {children}
      </AlertDescription>
    </Alert>
  );
}
