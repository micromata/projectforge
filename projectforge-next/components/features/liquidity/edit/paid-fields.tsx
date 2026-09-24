"use client";

import { useTranslations } from "next-intl";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { CheckboxField } from "@/components/shared/form/checkbox-field";
import {
  FieldShell,
  useFieldIds,
  type FieldMetaState,
} from "@/components/shared/form/field-shell";
import { useEntityEditForm } from "@/components/shared/form/form-context";
import { useFieldErrors } from "@/components/shared/form/use-field-errors";
import { useFieldLabels } from "@/components/shared/form/use-field-labels";
import { LIQUIDITY_ENTRY_METADATA } from "@/lib/metadata/liquidity-entry.generated";
import { cn } from "@/lib/utils";

/** The select value standing for the "automatic" (null) paid state — never a boolean string. */
const PAID_AUTOMATIC = "auto";

/**
 * The paid status and the rule that derives it, together because they are one decision.
 *
 * `paid` is a three-state override, unlike the plain checkbox its BOOLEAN metadata would build: it is
 * "automatic" (null), "paid" (true) or "not paid" (false). Automatic follows [autoSetPaid] — once set,
 * the entry counts as paid the day after its date of payment — so a select rather than a checkbox, which
 * could only say true/false and never "leave it to the rule". The backend computes the same,
 * `effectivePaid = paid ?? (autoSetPaid && dateOfPayment < today)`.
 */
export function PaidFields({ className }: { className?: string }) {
  const t = useTranslations();
  const label = useFieldLabels(LIQUIDITY_ENTRY_METADATA);
  const form = useEntityEditForm();
  const fieldErrors = useFieldErrors();
  const ids = useFieldIds();
  return (
    <div className={cn("flex flex-col gap-4", className)}>
      <form.Field name={"paid" as never}>
        {/* eslint-disable-next-line @typescript-eslint/no-explicit-any */}
        {(field: any) => {
          const meta = field.state.meta as FieldMetaState;
          const invalid = meta.isTouched && !meta.isValid;
          const value = field.state.value as boolean | null | undefined;
          const raw = value == null ? PAID_AUTOMATIC : String(value);
          return (
            <FieldShell
              name="paid"
              label={label("paid")}
              hint={t("plugins.liquidityplanning.entry.paid.info")}
              invalid={invalid}
              errors={fieldErrors(meta, label("paid"))}
              ids={ids}
            >
              <Select
                value={raw}
                onValueChange={(v) =>
                  field.handleChange(v === PAID_AUTOMATIC ? null : v === "true")
                }
              >
                <SelectTrigger
                  id={ids.controlId}
                  aria-labelledby={ids.labelId}
                  className="min-w-0"
                >
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value={PAID_AUTOMATIC}>
                    {t("plugins.liquidityplanning.entry.paid.automatic")}
                  </SelectItem>
                  <SelectItem value="true">
                    {t("plugins.liquidityplanning.entry.paid.paid")}
                  </SelectItem>
                  <SelectItem value="false">
                    {t("plugins.liquidityplanning.entry.paid.unpaid")}
                  </SelectItem>
                </SelectContent>
              </Select>
            </FieldShell>
          );
        }}
      </form.Field>
      <CheckboxField
        name="autoSetPaid"
        label={label("autoSetPaid")}
        hint={t("plugins.liquidityplanning.entry.autoSetPaid.info")}
      />
    </div>
  );
}
