"use client";

import { useEffect } from "react";
import { useStore } from "@tanstack/react-form";
import { useTranslations } from "next-intl";
import { InputField } from "@/components/shared/form/input-field";
import { useEntityEditForm } from "@/components/shared/form/form-context";
import { NumberField } from "@/components/shared/form/number-field";
import { useFieldLabels } from "@/components/shared/form/use-field-labels";
import { useFormatContext } from "@/hooks/use-format";
import { daysBetweenDates, shiftDateByDays } from "@/lib/date-parse";
import { RECHNUNG_METADATA } from "@/lib/metadata/rechnung.generated";
import { cn } from "@/lib/utils";
import type { InvoiceValues } from "../invoice-schema";

/** A date of the invoice and the day count from `datum` to it — the two ways to state one term. */
const DERIVED_TARGETS = [
  { date: "faelligkeit", days: "zahlungsZielInTagen" },
  { date: "discountMaturity", days: "discountZahlungsZielInTagen" },
] as const;

/**
 * When the invoice is due, when a discount would still apply, and what was actually paid.
 *
 * Custom rather than declared because of one rule between fields: a payment target in days and the date
 * it leads to say the same thing twice, so the two are kept on each other here — entering days moves the
 * date, moving the date rewrites the days.
 *
 * The days are typed only while the invoice is **new**, and read what the dates say from then on — the same
 * split as `AbstractRechnungEditForm` (a dropdown while the date is empty, a read-only text after that), and
 * for its reason: from the second the invoice has dates, those are what it is judged by, and the days are the
 * formula they came out of. They stay on screen and stay part of what is saved, because they are the number
 * an invoice is actually agreed in ("30 days net") and the number a clone is rebuilt from
 * (`OutgoingInvoiceEntityRest.prepareInvoiceClone`) — hiding them left an opened invoice looking as if it
 * had no payment term at all.
 *
 * The days are free to type rather than picked from Wicket's `ZAHLUNGSZIELE_IN_TAGEN` list: every term
 * that list doesn't happen to contain is as valid as the five that are on it.
 */
export function PaymentTermsFields({
  id,
  className,
}: {
  /** The stored invoice, or null while adding one — which is when the days are a formula, see above. */
  id: number | null;
  className?: string;
}) {
  const t = useTranslations();
  const label = useFieldLabels(RECHNUNG_METADATA);
  const format = useFormatContext();
  const form = useEntityEditForm();
  const isNew = id == null;

  // The invoice date and the two dates derived from it; everything else here re-renders on its own
  // field's change.
  const { datum, faelligkeit, discountMaturity } = useStore(
    form.store,
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    (state: any) => {
      const v = state.values as InvoiceValues;
      return {
        datum: v.datum,
        faelligkeit: v.faelligkeit,
        discountMaturity: v.discountMaturity,
      };
    }
  );

  // Whenever one of the dates moves — the due date here, or the invoice date in the section above — the
  // day counts follow, so what the boxes say stays one term and not two. Loading an invoice changes
  // nothing: `toFormValues` derives the same numbers, and only a differing value is written.
  useEffect(() => {
    for (const { date, days } of DERIVED_TARGETS) {
      const derived = daysBetweenDates(
        datum,
        form.getFieldValue(date) as string | null
      );
      if (derived != null && form.getFieldValue(days) !== derived) {
        form.setFieldValue(days, derived);
      }
    }
  }, [form, datum, faelligkeit, discountMaturity]);

  /**
   * The other direction: days typed into a box become the date the invoice is judged by — the backend
   * only derives it itself while the date is empty (`AuftragAndRechnungDaoHelper.onSaveOrModify`), so a
   * changed term would otherwise leave the old date standing.
   *
   * An emptied box leaves the date alone: deleting a number is how one is retyped, and dropping the due
   * date of an issued invoice over a keystroke is not what anybody means by it.
   */
  const moveDate = (
    date: (typeof DERIVED_TARGETS)[number]["date"],
    days: number | null
  ) => {
    if (days == null) return;
    const next = shiftDateByDays(datum, days);
    if (next) form.setFieldValue(date, next);
  };

  return (
    <div
      className={cn(
        "grid grid-cols-1 gap-x-6 gap-y-4 md:grid-cols-3",
        className
      )}
    >
      <InputField name="faelligkeit" label={label("faelligkeit")} type="date" />
      <NumberField
        name="zahlungsZielInTagen"
        label={label("zahlungsZielInTagen")}
        suffix={t("days")}
        // A term is counted in days, and no term needs four digits.
        maxDigits={3}
        disabled={!isNew}
        onChanged={(days) => moveDate("faelligkeit", days)}
      />
      <InputField
        name="discountMaturity"
        label={label("discountMaturity")}
        type="date"
        // Starts a row of its own: the discount is a block of three fields, and letting it begin
        // wherever the row above happened to end would read as part of the due date beside it.
        className="md:col-start-1"
      />
      <NumberField
        name="discountZahlungsZielInTagen"
        label={label("discountZahlungsZielInTagen")}
        suffix={t("days")}
        maxDigits={3}
        disabled={!isNew}
        onChanged={(days) => moveDate("discountMaturity", days)}
      />
      <NumberField
        name="discountPercent"
        label={label("discountPercent")}
        // Already a percentage in the entity, unlike a position's VAT factor — so no conversion, only
        // the sign behind the box.
        fractionDigits={2}
        suffix="%"
        maxDigits={5}
      />
      {/* What was actually paid, and when — a row of its own for the same reason as the discount. */}
      <InputField
        name="bezahlDatum"
        label={label("bezahlDatum")}
        type="date"
        className="md:col-start-1"
      />
      <NumberField
        name="zahlBetrag"
        label={label("zahlBetrag")}
        // DECIMAL, not AMOUNT — `AbstractRechnungDO.zahlBetrag` is a plain `BigDecimal`.
        fractionDigits={2}
        suffix={format.currency}
      />
    </div>
  );
}
