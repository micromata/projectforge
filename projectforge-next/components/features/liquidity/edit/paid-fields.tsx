"use client";

import { useTranslations } from "next-intl";
import { useStore } from "@tanstack/react-form";
import { SelectContent, SelectItem, SelectValue } from "@/components/ui/select";
import { Select, SelectTrigger } from "@/components/shared/copyable-select";
import { SelectItemWithHint } from "@/components/shared/form/select-item-with-hint";
import {
  FieldShell,
  useFieldIds,
  type FieldMetaState,
} from "@/components/shared/form/field-shell";
import { useEntityEditForm } from "@/components/shared/form/form-context";
import { useFieldErrors } from "@/components/shared/form/use-field-errors";
import { useFieldLabels } from "@/components/shared/form/use-field-labels";
import { LIQUIDITY_ENTRY_METADATA } from "@/lib/metadata/liquidity-entry.generated";

/** The select value standing for the "automatic" state — never a boolean string. */
const PAID_AUTOMATIC = "auto";

/**
 * The paid status as a single three-state select — the only control for it, folding in what used to be a
 * separate `autoSetPaid` checkbox beside it. Its three states are the only ones a user can meaningfully
 * distinguish, because `effectivePaid = paid ?? (autoSetPaid && dateOfPayment < today)`:
 *
 * - "automatic" → `paid = null`, `autoSetPaid = true`: no manual override, counts as paid once its date of
 *   payment has passed.
 * - "paid" → `paid = true`: forced paid.
 * - "not paid" → `paid = false`: forced unpaid.
 *
 * `paid = null` with `autoSetPaid = false` is effectively "not paid" (`effectivePaid` is always false), so it
 * reads as "not paid" here and needs no fourth option — that redundant fourth combination was the confusing
 * duplication of a `paid` select plus an `autoSetPaid` checkbox. Selecting a state writes both fields; the
 * backend still stores and computes them independently.
 *
 * A custom field (not derivable from metadata) so it can sit on the same row as the date and amount and drive
 * two form fields at once (see liquidity.page.tsx).
 */
export function PaidSelectField({ className }: { className?: string }) {
  const t = useTranslations();
  const label = useFieldLabels(LIQUIDITY_ENTRY_METADATA);
  const form = useEntityEditForm();
  const fieldErrors = useFieldErrors();
  const ids = useFieldIds();
  // The select value derives from both fields, so it must re-render when the sibling autoSetPaid changes too.
  const autoSetPaid = useStore(
    form.store,
    (state: unknown) =>
      (state as { values: { autoSetPaid?: boolean | null } }).values
        .autoSetPaid ?? false
  );
  return (
    <form.Field name={"paid" as never}>
      {/* eslint-disable-next-line @typescript-eslint/no-explicit-any */}
      {(field: any) => {
        const meta = field.state.meta as FieldMetaState;
        const invalid = meta.isTouched && !meta.isValid;
        const value = field.state.value as boolean | null | undefined;
        // null + autoSetPaid = "automatic"; null without it is effectively unpaid, so it reads as "false".
        const raw =
          value == null
            ? autoSetPaid
              ? PAID_AUTOMATIC
              : "false"
            : String(value);
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
              onValueChange={(v) => {
                // A closed shadcn Select's hidden native input fires "" on any controlled value change —
                // the form reset onto the loaded entity is one, and its <option>s exist only while the
                // dropdown is open, so a closed one matches nothing and posts "". Ignoring it is what
                // keeps a loaded status from being wiped to "not paid" (SelectField carries the same guard).
                if (v === "") return;
                field.handleChange(v === PAID_AUTOMATIC ? null : v === "true");
                form.setFieldValue(
                  "autoSetPaid" as never,
                  (v === PAID_AUTOMATIC) as never
                );
              }}
            >
              <SelectTrigger
                id={ids.controlId}
                aria-labelledby={ids.labelId}
                className="min-w-0"
              >
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                {/* The hint explains what "automatic" does — the note the removed autoSetPaid checkbox
                    used to carry — and is part of the clickable option (see SelectItemWithHint). */}
                <SelectItemWithHint
                  value={PAID_AUTOMATIC}
                  hint={t("plugins.liquidityplanning.entry.autoSetPaid.info")}
                >
                  {t("plugins.liquidityplanning.entry.paid.automatic")}
                </SelectItemWithHint>
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
