"use client";

import { useStore } from "@tanstack/react-form";
import { useTranslations } from "next-intl";
import { useEntityEditForm } from "@/components/shared/form/form-context";
import { FieldShell, useFieldIds } from "@/components/shared/form/field-shell";
import { NumberField } from "@/components/shared/form/number-field";
import { formatDate } from "@/lib/format";
import { useFormatContext } from "@/hooks/use-format";
import type { InvoiceValues } from "../invoice-schema";
import { InvoiceLink } from "./invoice-link";

/**
 * The invoice a cancellation cancels (`RechnungDO.originalRechnung`) — shown only while the type is
 * `CANCELLATION`, the one type the backend keeps the reference for.
 *
 * Two shapes, by whether the reference is resolved:
 * - It has an id — a stored cancellation, or one "Create cancellation" prepared: a link to the original,
 *   read-only. Changing it would leave positions behind that were negated off another invoice.
 * - It has none — a cancellation typed by hand: a box for the original's number, which the backend
 *   resolves (`OutgoingInvoiceEntityRest.transformForDB`) and `RechnungDao` checks (an invoice, not
 *   cancelled yet, net sum of the cancellation negative).
 */
export function OriginalInvoiceField({ className }: { className?: string }) {
  const t = useTranslations();
  const form = useEntityEditForm();
  const ids = useFieldIds();
  const format = useFormatContext();
  const { typ, original } = useStore(
    form.store,
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    (state: any) => {
      const v = state.values as InvoiceValues;
      return { typ: v.typ, original: v.originalInvoice };
    }
  );
  if (typ !== "CANCELLATION") return null;
  const label = t("fibu.rechnung.originalRechnung");

  if (original?.id == null) {
    return (
      <NumberField
        name="originalInvoice.nummer"
        label={label}
        maxDigits={8}
        metadataLess
        className={className}
      />
    );
  }
  const number = original.nummer ?? original.id;
  return (
    <FieldShell
      name="originalInvoice"
      label={label}
      readOnly
      invalid={false}
      errors={[]}
      className={className}
      ids={ids}
    >
      <div id={ids.controlId} className="flex h-9 items-center gap-2 text-sm">
        <InvoiceLink
          invoiceId={original.id}
          ariaLabel={`${t("show")}: ${t("fibu.rechnung")} ${number}`}
          className="font-semibold tabular-nums"
        >
          {number}
        </InvoiceLink>
        {original.datum && (
          <span className="text-muted-foreground">
            {formatDate(original.datum, format)}
          </span>
        )}
      </div>
    </FieldShell>
  );
}
