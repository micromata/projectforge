"use client";

import { useStore } from "@tanstack/react-form";
import { useTranslations } from "next-intl";
import { useEntityEditForm } from "@/components/shared/form/form-context";
import { FieldShell, useFieldIds } from "@/components/shared/form/field-shell";
import { NumberField } from "@/components/shared/form/number-field";
import { leafKeyOf } from "@/lib/leaf-key";
import type { InvoiceValues } from "../invoice-schema";

/**
 * The invoice number (`RechnungDO.nummer`) — a custom field because it depends on the type, which the
 * static `readOnly` of `page-def` cannot follow.
 *
 * - Any invoice but a cancellation: the number box. Assigned by `RechnungDao.onInsertOrModify` on the
 *   transition out of GEPLANT, and absent from a credit note the customer announced — but editable, as in
 *   Wicket: an invoice issued by mistake is set back to planned, and then its number has to go as well, or
 *   it still names an invoice that no longer claims to be issued. Leaving it empty on a new invoice is the
 *   normal case and what the hint says; a number that isn't the next free one is `RechnungDao`'s to refuse
 *   (`rechnungsNummerIstNichtFortlaufend`, `rechnungsNummerBereitsVergeben`).
 * - A cancellation: read-only, it has no number of its own. Shown is the synthetic one it is known by, the
 *   cancelled invoice's plus "-S" (`RechnungDO.belegNummer`), derived live from the original's number.
 *   Unless the form still holds a number — a numbered invoice whose type was switched to cancellation —
 *   which `RechnungDao` refuses (`cancellation.noOwnNumber`): then the box stays, so it can be emptied.
 */
export function InvoiceNumberField({ className }: { className?: string }) {
  const t = useTranslations();
  const form = useEntityEditForm();
  const ids = useFieldIds();
  const { typ, nummer, originalNummer } = useStore(
    form.store,
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    (state: any) => {
      const v = state.values as InvoiceValues;
      return {
        typ: v.typ,
        nummer: v.nummer,
        originalNummer: v.originalInvoice?.nummer ?? null,
      };
    }
  );
  const label = t(leafKeyOf("fibu.rechnung.nummer", t.has));

  if (typ !== "CANCELLATION" || nummer != null) {
    return (
      <NumberField
        name="nummer"
        label={label}
        hint={t("fibu.tooltip.nummerWirdAutomatischVergeben")}
        maxDigits={8}
        className={className}
      />
    );
  }
  return (
    <FieldShell
      name="nummer"
      label={label}
      readOnly
      invalid={false}
      errors={[]}
      className={className}
      ids={ids}
    >
      <div
        id={ids.controlId}
        className="flex h-9 items-center text-sm font-semibold tabular-nums"
      >
        {originalNummer != null ? `${originalNummer}-S` : "—"}
      </div>
    </FieldShell>
  );
}
