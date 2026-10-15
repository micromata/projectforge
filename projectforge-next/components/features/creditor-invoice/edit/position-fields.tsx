"use client";

import { InputField } from "@/components/shared/form/input-field";
import { NumberField } from "@/components/shared/form/number-field";
import { useFieldLabels } from "@/components/shared/form/use-field-labels";
import { useFormatContext } from "@/hooks/use-format";
import { EINGANGSRECHNUNGS_POSITION_METADATA } from "@/lib/metadata/eingangsrechnungs-position.generated";
import { PositionSumsLine } from "@/components/shared/invoice/position-sums-line";
import type { InvoicePositionSums } from "@/lib/rs/invoice-sums";

/**
 * The fields of one incoming invoice position, everything but its cost split: what is billed, how much of
 * it at what unit price and VAT rate.
 *
 * Leaner than the outgoing invoice's [PositionFields]: a creditor invoice bills no order and states no
 * period of performance of its own, so neither the order picker nor the period fields are here.
 */
export function PositionFields({
  prefix,
  sums,
}: {
  /** Prefix of every field name of this row, e.g. `positionen[2].`. */
  prefix: string;
  sums: InvoicePositionSums | undefined;
}) {
  const label = useFieldLabels(EINGANGSRECHNUNGS_POSITION_METADATA);
  const format = useFormatContext();
  const name = (field: string) => `${prefix}${field}`;

  return (
    <>
      <InputField
        name={name("text")}
        label={label("text")}
        className="md:col-span-3"
      />
      <NumberField
        name={name("menge")}
        label={label("menge")}
        fractionDigits={2}
      />
      <NumberField
        name={name("einzelNetto")}
        label={label("einzelNetto")}
        // DECIMAL, not AMOUNT: `EingangsrechnungsPositionDO.einzelNetto` is a plain `BigDecimal`, so the
        // currency and the two digits are passed explicitly rather than derived from the data type.
        fractionDigits={2}
        suffix={format.currency}
      />
      <NumberField
        name={name("vat")}
        label={label("vat")}
        // Stored as a factor, entered as a percentage — see [NumberFieldProps.percent].
        percent
        fractionDigits={2}
        suffix="%"
        maxDigits={5}
      />
      <PositionSumsLine sums={sums} className="md:col-span-3" />
    </>
  );
}
