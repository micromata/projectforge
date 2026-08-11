"use client";

import { useTranslations } from "next-intl";
import { CheckboxField } from "@/components/shared/form/checkbox-field";
import { InputField } from "@/components/shared/form/input-field";
import { NestedFieldMetadata } from "@/components/shared/form/form-context";
import { NumberField } from "@/components/shared/form/number-field";
import { RepeatableRow } from "@/components/shared/form/repeatable-row";
import { SelectField } from "@/components/shared/form/select-field";
import { useFieldLabels } from "@/components/shared/form/use-field-labels";
import { useFormatContext } from "@/hooks/use-format";
import { formatCurrency, formatDate } from "@/lib/format";
import { PAYMENT_SCHEDULE_METADATA } from "@/lib/metadata/payment-schedule.generated";
import type { PaymentScheduleValues } from "../order-schema";

export interface PaymentScheduleRowProps {
  schedule: PaymentScheduleValues;
  index: number;
  /** Prefix of every field name of this row, e.g. `paymentSchedules[1].`. */
  prefix: string;
  /** The positions to pick from, as `{value, label}` — an instalment refers to one by its number. */
  positionOptions: { value: string; label: string }[];
  onRemove?: () => void;
  /** Whether the `vollstaendigFakturiert` checkbox may be shown (FIBU right, as for a position). */
  invoiceWriteAccess: boolean;
}

/**
 * One instalment of the payment schedule: when how much is due for which position, and whether that
 * milestone was reached.
 *
 * `positionNumber` refers to a position's **number**, not its id (`PaymentScheduleDO`), which is why the
 * select is built from the form's positions rather than from an autocomplete: the numbers only exist
 * within this order, and a new position has none until it is saved.
 */
export function PaymentScheduleRow({
  schedule,
  index,
  prefix,
  positionOptions,
  onRemove,
  invoiceWriteAccess,
}: PaymentScheduleRowProps) {
  const t = useTranslations();
  const label = useFieldLabels(PAYMENT_SCHEDULE_METADATA);
  const format = useFormatContext();
  const name = (field: string) => `${prefix}${field}`;

  return (
    <NestedFieldMetadata
      metadata={PAYMENT_SCHEDULE_METADATA}
      namePrefix={prefix}
    >
      <RepeatableRow
        header={
          <span className="flex min-w-0 flex-1 items-center gap-2 text-sm">
            <span className="shrink-0 text-muted-foreground">
              {schedule.number ?? index + 1}
            </span>
            <span className="shrink-0 tabular-nums">
              {formatDate(schedule.scheduleDate, format)}
            </span>
            <span className="min-w-0 flex-1 truncate text-muted-foreground">
              {schedule.comment}
            </span>
            <span className="shrink-0 tabular-nums">
              {formatCurrency(schedule.amount, format)}
            </span>
          </span>
        }
        defaultOpen={schedule.id == null}
        onRemove={onRemove}
        removeLabel={`${t("fibu.auftrag.paymentschedule._")} ${schedule.number ?? index + 1}`}
      >
        <InputField
          name={name("scheduleDate")}
          label={label("scheduleDate")}
          type="date"
        />
        <NumberField
          name={name("amount")}
          label={label("amount")}
          // DECIMAL rather than AMOUNT, as on the position — the currency is passed explicitly.
          fractionDigits={2}
          suffix={format.currency}
        />
        <SelectField
          name={name("positionNumber")}
          label={label("positionNumber")}
          options={positionOptions}
          numeric
        />
        <InputField
          name={name("comment")}
          label={label("comment")}
          className="md:col-span-2"
        />
        <CheckboxField name={name("reached")} label={label("reached")} />
        {invoiceWriteAccess && (
          <CheckboxField
            name={name("vollstaendigFakturiert")}
            label={label("vollstaendigFakturiert")}
          />
        )}
      </RepeatableRow>
    </NestedFieldMetadata>
  );
}
