"use client";

import { useTranslations } from "next-intl";
import { DatePeriodField } from "@/components/shared/form/date-period-field";
import { InputField } from "@/components/shared/form/input-field";
import { NestedFieldMetadata } from "@/components/shared/form/form-context";
import { NumberField } from "@/components/shared/form/number-field";
import { RepeatableRow } from "@/components/shared/form/repeatable-row";
import { SelectField } from "@/components/shared/form/select-field";
import { useFieldLabels } from "@/components/shared/form/use-field-labels";
import { useFormatContext } from "@/hooks/use-format";
import { RECHNUNGS_POSITION_METADATA } from "@/lib/metadata/rechnungs-position.generated";
import { fromMetadata } from "@/lib/validation/from-metadata";
import { CostAssignmentsSection } from "./cost-assignments-section";
import { PositionRowHeader } from "./position-row-header";
import { PositionSumsLine } from "./position-sums-line";
import type { InvoicePositionValues } from "../invoice-schema";
import type { InvoicePositionSums } from "@/lib/rs/invoice";

const p = fromMetadata(RECHNUNGS_POSITION_METADATA);

export interface PositionRowProps {
  position: InvoicePositionValues;
  /** Index in the form's `positionen` array — the row's field names are built from it. */
  index: number;
  /** Prefix of every field name of this row, e.g. `positionen[2].`. */
  prefix: string;
  sums: InvoicePositionSums | undefined;
  /** Absent where the invoice may not be written. */
  onRemove?: () => void;
  onRestore?: () => void;
  /**
   * Whether cost accounting is configured at all (`Configuration.isCostConfigured`, carried by the DTO
   * as `costConfigured`). False hides the cost assignments entirely, as Wicket's form does.
   */
  costConfigured: boolean;
}

/**
 * One invoice position: what is billed, how much of it at what unit price and VAT rate, the period it
 * covers, and how its net sum is split across cost units.
 *
 * Hand-written JSX rather than a declaration, for the reason the order's [PositionRow] is: what a
 * position looks like — a period that appears only when it has its own, a nested table of cost
 * assignments — is this entity's business. Every *mechanism* is the shared one: the collapsing row, the
 * fields, and their labels and rules from the position's own metadata ([NestedFieldMetadata]).
 */
export function PositionRow({
  position,
  index,
  prefix,
  sums,
  onRemove,
  onRestore,
  costConfigured,
}: PositionRowProps) {
  const t = useTranslations();
  const label = useFieldLabels(RECHNUNGS_POSITION_METADATA);
  const format = useFormatContext();
  const name = (field: string) => `${prefix}${field}`;
  // The invoice's own period applies unless the position was given one — then, and only then, are the
  // two date fields hers to fill in (`PeriodOfPerformanceType`, and Wicket's form does the same).
  const ownPeriod = position.periodOfPerformanceType === "OWN";
  const writeAccess = !!onRemove;

  return (
    <NestedFieldMetadata
      metadata={RECHNUNGS_POSITION_METADATA}
      namePrefix={prefix}
    >
      <RepeatableRow
        header={
          <PositionRowHeader
            position={position}
            sums={sums}
            costConfigured={costConfigured}
          />
        }
        // A row just added is there to be filled in; a stored one stays folded, which is what makes an
        // invoice of a dozen positions readable at all.
        defaultOpen={position.id == null}
        deleted={position.deleted}
        onRemove={onRemove}
        onRestore={onRestore}
        removeLabel={
          position.text ??
          `${t("label.position.short")} ${position.number ?? index + 1}`
        }
        // Marked as needing attention where the cost assignments don't add up — the red Wicket paints.
        highlighted={
          costConfigured &&
          sums?.kostZuweisungNetFehlbetrag != null &&
          sums.kostZuweisungNetFehlbetrag !== 0
        }
      >
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
            className="md:col-span-2"
          />
        )}
        {costConfigured && (
          <CostAssignmentsSection
            prefix={prefix}
            sums={sums}
            writeAccess={writeAccess}
            className="md:col-span-3"
          />
        )}
      </RepeatableRow>
    </NestedFieldMetadata>
  );
}
