"use client";

import { useTranslations } from "next-intl";
import { DatePeriodField } from "@/components/shared/form/date-period-field";
import { InputField } from "@/components/shared/form/input-field";
import { NumberField } from "@/components/shared/form/number-field";
import { SelectField } from "@/components/shared/form/select-field";
import { useFieldLabels } from "@/components/shared/form/use-field-labels";
import { useFormatContext } from "@/hooks/use-format";
import { TERM_KIND_IDS } from "@/lib/date-period";
import { RECHNUNGS_POSITION_METADATA } from "@/lib/metadata/rechnungs-position.generated";
import { fromMetadata } from "@/lib/validation/from-metadata";
import { OrderPositionField } from "./order-position-field";
import { PositionSumsLine } from "@/components/shared/invoice/position-sums-line";
import type { InvoicePositionValues } from "../invoice-schema";
import type { InvoicePositionSums } from "@/lib/rs/invoice";

const p = fromMetadata(RECHNUNGS_POSITION_METADATA);

/**
 * The fields of one invoice position, everything but its cost split: what is billed, how much of it at
 * what unit price and VAT rate, which order position it invoices, and the period it covers.
 *
 * Split out of [PositionRow], which is the collapsing row around this and the cost assignments — two
 * responsibilities that together no longer fit one file.
 */
export function PositionFields({
  position,
  prefix,
  sums,
}: {
  position: InvoicePositionValues;
  /** Prefix of every field name of this row, e.g. `positionen[2].`. */
  prefix: string;
  sums: InvoicePositionSums | undefined;
}) {
  const t = useTranslations();
  const label = useFieldLabels(RECHNUNGS_POSITION_METADATA);
  const format = useFormatContext();
  const name = (field: string) => `${prefix}${field}`;
  // The invoice's own period applies unless the position was given one — then, and only then, are the
  // two date fields hers to fill in (`PeriodOfPerformanceType`, and Wicket's form does the same).
  const ownPeriod = position.periodOfPerformanceType === "OWN";

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
        // DECIMAL, not AMOUNT: `RechnungsPositionDO.einzelNetto` is a plain `BigDecimal`, so the
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
      <OrderPositionField
        name={name("auftragsPosition")}
        className="md:col-span-2"
      />
      <SelectField
        name={name("periodOfPerformanceType")}
        label={label("periodOfPerformanceType")}
        options={p.enumOptions("periodOfPerformanceType", t)}
      />
      {ownPeriod && (
        <DatePeriodField
          label={t("fibu.periodOfPerformance._")}
          begin={{
            name: name("periodOfPerformanceBegin"),
            label: label("periodOfPerformanceBegin"),
          }}
          end={{
            name: name("periodOfPerformanceEnd"),
            label: label("periodOfPerformanceEnd"),
          }}
          periodKinds={TERM_KIND_IDS}
          paging
          className="md:col-span-2"
        />
      )}
    </>
  );
}
