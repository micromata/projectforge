"use client";

import { useStore } from "@tanstack/react-form";
import { useTranslations } from "next-intl";
import { InputField } from "@/components/shared/form/input-field";
import { useEntityEditForm } from "@/components/shared/form/form-context";
import { NumberField } from "@/components/shared/form/number-field";
import { SelectField } from "@/components/shared/form/select-field";
import { useFieldLabels } from "@/components/shared/form/use-field-labels";
import { useFormatContext } from "@/hooks/use-format";
import { RECHNUNG_METADATA } from "@/lib/metadata/rechnung.generated";
import { cn } from "@/lib/utils";
import type { InvoiceValues } from "../invoice-schema";

/** The payment targets Wicket offers (`ZAHLUNGSZIELE_IN_TAGEN`) — not a range but the usual terms. */
const PAYMENT_TARGETS_IN_DAYS = [7, 14, 30, 60, 90];

/**
 * When the invoice is due, when a discount would still apply, and what was actually paid.
 *
 * Custom rather than declared because of one rule between fields: a payment target in days and a due
 * date say the same thing twice. `AuftragAndRechnungDaoHelper.onSaveOrModify` derives the date from the
 * days, so the days are offered only while the date is empty — as `AbstractRechnungEditForm` does, and
 * for the better reason that a user who has entered a date should not be shown a box that would silently
 * move it.
 */
export function PaymentTermsFields({ className }: { className?: string }) {
  const t = useTranslations();
  const label = useFieldLabels(RECHNUNG_METADATA);
  const format = useFormatContext();
  const form = useEntityEditForm();

  // Only the two dates: everything else here re-renders on its own field's change.
  const { faelligkeit, discountMaturity } = useStore(
    form.store,
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    (state: any) => {
      const v = state.values as InvoiceValues;
      return {
        faelligkeit: v.faelligkeit,
        discountMaturity: v.discountMaturity,
      };
    }
  );

  const targetOptions = PAYMENT_TARGETS_IN_DAYS.map((days) => ({
    value: String(days),
    label: `${days} ${t("days")}`,
  }));

  return (
    <div
      className={cn(
        "grid grid-cols-1 gap-x-6 gap-y-4 md:grid-cols-3",
        className
      )}
    >
      <InputField name="faelligkeit" label={label("faelligkeit")} type="date" />
      {/* Gone as soon as a due date is there — it would derive a second one. */}
      {!faelligkeit && (
        <SelectField
          name="zahlungsZielInTagen"
          label={label("zahlungsZielInTagen")}
          options={targetOptions}
          numeric
        />
      )}
      <InputField
        name="discountMaturity"
        label={label("discountMaturity")}
        type="date"
        // Starts a row of its own: the discount is a block of three fields, and letting it begin
        // wherever the row above happened to end would read as part of the due date beside it.
        className="md:col-start-1"
      />
      {!discountMaturity && (
        <SelectField
          name="discountZahlungsZielInTagen"
          label={label("discountZahlungsZielInTagen")}
          options={targetOptions}
          numeric
        />
      )}
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
