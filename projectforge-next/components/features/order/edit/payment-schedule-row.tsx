"use client";

import { useTranslations } from "next-intl";
import { CollapsibleSummary } from "@/components/shared/collapsible-summary";
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
  /** Prefix of every field name of this row, e.g. `paymentSchedules[1].`. */
  prefix: string;
  /** The positions to pick from, as `{value, label}` — an instalment refers to one by its number. */
  positionOptions: { value: string; label: string }[];
  onRemove?: () => void;
  /** Takes a soft-deleted instalment back — see [RepeatableRow], which renders the deleted state. */
  onRestore?: () => void;
  /**
   * Whether the `vollstaendigFakturiert` checkbox may be ticked — the invoice right and PF_Finance, as for
   * a position. False shows it read-only rather than hiding it.
   */
  invoiceFlagWriteAccess: boolean;
}

/**
 * One instalment of the payment schedule: when how much is due for which position, and whether that
 * milestone was reached.
 *
 * `positionNumber` refers to a position's **number**, not its id (`PaymentScheduleDO`), which is why the
 * select is built from the form's positions rather than from an autocomplete: the numbers only exist
 * within this order, and a new position has none until it is saved.
 *
 * The header is a [CollapsibleSummary], as a position's is: due date and amount identify the instalment,
 * everything else is shown only while the row is folded.
 */
export function PaymentScheduleRow({
  schedule,
  prefix,
  positionOptions,
  onRemove,
  onRestore,
  invoiceFlagWriteAccess,
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
        highlighted={!!schedule.reached && !schedule.vollstaendigFakturiert}
        header={
          <CollapsibleSummary
            // What identifies an instalment: which one it is, when it is due, and for how much.
            primary={
              <>
                <span className="shrink-0 text-muted-foreground">
                  {schedule.number}
                </span>
                <span className="shrink-0 tabular-nums">
                  {formatDate(schedule.scheduleDate, format)}
                </span>
                {/* Right-hand end of the line, as the sum of a position is. */}
                <span className="ml-auto shrink-0 tabular-nums">
                  {formatCurrency(schedule.amount, format)}
                </span>
              </>
            }
            details={[
              schedule.comment,
              schedule.positionNumber != null &&
                `${t("label.position.short")} ${schedule.positionNumber}`,
              schedule.reached && (
                <span className="text-green-600 dark:text-green-400">
                  {t("fibu.common.reached")}
                </span>
              ),
            ]}
          />
        }
        defaultOpen={schedule.id == null}
        deleted={schedule.deleted}
        onRemove={onRemove}
        onRestore={onRestore}
        removeLabel={`${t("fibu.auftrag.paymentschedule._")} ${schedule.number}`}
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
        {/* Always rendered, read-only without the right — as in a position (PositionRow). */}
        <CheckboxField
          name={name("vollstaendigFakturiert")}
          label={label("vollstaendigFakturiert")}
          disabled={!invoiceFlagWriteAccess}
          hint={
            invoiceFlagWriteAccess
              ? undefined
              : t("fibu.auftrag.error.vollstaendigFakturiertProtection")
          }
        />
      </RepeatableRow>
    </NestedFieldMetadata>
  );
}
