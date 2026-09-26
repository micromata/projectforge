"use client";

import { useTranslations } from "next-intl";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  FieldShell,
  useFieldIds,
  type FieldMetaState,
} from "@/components/shared/form/field-shell";
import { useEntityEditForm } from "@/components/shared/form/form-context";
import { useFieldErrors } from "@/components/shared/form/use-field-errors";
import { useFieldLabels } from "@/components/shared/form/use-field-labels";
import { LIQUIDITY_ENTRY_METADATA } from "@/lib/metadata/liquidity-entry.generated";

/** The select value standing for the "automatic" (null) paid state — never a boolean string. */
const PAID_AUTOMATIC = "auto";

/**
 * The three-state paid override as a select, unlike the plain checkbox its BOOLEAN metadata would build:
 * "automatic" (null), "paid" (true) or "not paid" (false). Automatic follows the `autoSetPaid` rule
 * declared beside it — once set, the entry counts as paid the day after its date of payment — so a select
 * rather than a checkbox, which could only say true/false and never "leave it to the rule". The backend
 * computes the same, `effectivePaid = paid ?? (autoSetPaid && dateOfPayment < today)`.
 *
 * A custom field (not derivable from metadata) so it can sit on the same row as the date, amount and the
 * autoSetPaid checkbox (see liquidity.page.tsx).
 */
export function PaidSelectField({ className }: { className?: string }) {
  const t = useTranslations();
  const label = useFieldLabels(LIQUIDITY_ENTRY_METADATA);
  const form = useEntityEditForm();
  const fieldErrors = useFieldErrors();
  const ids = useFieldIds();
  return (
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
            className={className}
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
  );
}
