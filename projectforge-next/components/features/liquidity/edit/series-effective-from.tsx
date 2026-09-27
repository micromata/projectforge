"use client";

import { useStore } from "@tanstack/react-form";
import { useTranslations } from "next-intl";
import { useSearchParams } from "next/navigation";
import { RadioGroup, RadioGroupItem } from "@/components/ui/radio-group";
import { Label } from "@/components/ui/label";
import { InputField } from "@/components/shared/form/input-field";
import {
  FieldShell,
  useFieldIds,
  type FieldMetaState,
} from "@/components/shared/form/field-shell";
import { useEntityEditForm } from "@/components/shared/form/form-context";
import { useFieldErrors } from "@/components/shared/form/use-field-errors";
import { cn } from "@/lib/utils";
import type { LiquiditySeriesValues } from "../liquidity-series-schema";

/** Today as an ISO `YYYY-MM-DD` string, comparable to the date strings the form carries. */
function todayIso(): string {
  return new Date().toLocaleDateString("sv-SE");
}

/**
 * The scope control of the series editor — the deliberate "which occurrences does this change touch"
 * choice, an inline required radio like the team-event `SeriesModificationSection`.
 *
 * The two modes map to the posted transient fields: "whole series" (`changeScope="WHOLE"`,
 * `effectiveFrom=null`) is a plain in-place update — every still-virtual occurrence, past and future,
 * follows the new template; "from a date on" (`changeScope="SPLIT"`, `effectiveFrom=<date>`) is the split
 * the Save routes to (see liquidity-series.page.tsx and LiquiditySeriesRest.split). The split date is
 * seeded from the clicked occurrence's anchor (`?from=…`, set by SeriesLink), falling back to today, and
 * stays freely editable — including into the future ("the rent rises on the 1st of next month").
 *
 * For a stored series that has already started the choice is required (the schema anticipates the same
 * refusal): without it a whole-series edit would silently rewrite the realized past. A series entirely in
 * the future needs no guard — "whole series" is the harmless default. When "whole series" is chosen with a
 * started series, an inline warning spells out that past occurrences change too (allowed, not blocked).
 */
export function SeriesEffectiveFrom({ className }: { className?: string }) {
  const t = useTranslations();
  const form = useEntityEditForm();
  const fieldErrors = useFieldErrors();
  const ids = useFieldIds();
  const from = useSearchParams().get("from");

  const values = useStore(form.store, (s: unknown) => (s as FormState).values);
  const { id, startDate, effectiveFrom, changeScope } = values;

  // The series has realized (past) occurrences the whole-series edit would rewrite; only then is the
  // choice forced. A brand-new or future-only series can be changed in place without asking.
  const hasPast = id != null && startDate != null && startDate < todayIso();
  const selected = changeScope ?? (hasPast ? "" : "WHOLE");

  return (
    <form.Field name={"changeScope" as never}>
      {/* eslint-disable-next-line @typescript-eslint/no-explicit-any */}
      {(field: any) => {
        const meta = field.state.meta as FieldMetaState;
        const label = t("plugins.liquidityplanning.series.scope.text");
        return (
          <FieldShell
            name="changeScope"
            label={label}
            required={hasPast}
            invalid={meta.isTouched && !meta.isValid}
            errors={fieldErrors(meta, label)}
            ids={ids}
            className={cn("flex flex-col gap-2", className)}
          >
            <RadioGroup
              value={selected}
              onValueChange={(v) => {
                field.handleChange(v);
                form.setFieldValue(
                  "effectiveFrom" as never,
                  (v === "SPLIT"
                    ? (effectiveFrom ?? from ?? todayIso())
                    : null) as never
                );
              }}
              className="gap-2"
            >
              <div className="flex flex-col gap-1">
                <div className="flex items-center gap-2">
                  <RadioGroupItem id="series-scope-whole" value="WHOLE" />
                  <Label
                    htmlFor="series-scope-whole"
                    className="text-sm font-normal text-foreground"
                  >
                    {t("plugins.liquidityplanning.series.scope.whole")}
                  </Label>
                </div>
                {selected === "WHOLE" && hasPast && (
                  <p className="ml-6 text-sm text-destructive">
                    {t(
                      "plugins.liquidityplanning.series.scope.wholePastWarning"
                    )}
                  </p>
                )}
              </div>
              <div className="flex items-center gap-2">
                <RadioGroupItem id="series-scope-fromDate" value="SPLIT" />
                <Label
                  htmlFor="series-scope-fromDate"
                  className="text-sm font-normal text-foreground"
                >
                  {t("plugins.liquidityplanning.series.scope.fromDate")}
                </Label>
              </div>
            </RadioGroup>
            {selected === "SPLIT" && (
              <div className="flex flex-col gap-1">
                <InputField
                  name="effectiveFrom"
                  type="date"
                  metadataLess
                  label={t("plugins.liquidityplanning.series.effectiveFrom")}
                  className="max-w-xs"
                />
                {/* The interval stays fixed in split mode: the continuation keeps the original grid so the
                    re-linked occurrences stay aligned (enforced in LiquiditySeriesRest.split). */}
                <p className="text-sm text-muted-foreground">
                  {t("plugins.liquidityplanning.series.scope.fromDateHint")}
                </p>
              </div>
            )}
          </FieldShell>
        );
      }}
    </form.Field>
  );
}

/** The slice of the form store read here; the context is deliberately untyped (form-context). */
interface FormState {
  values: Pick<
    LiquiditySeriesValues,
    "id" | "startDate" | "effectiveFrom" | "changeScope"
  >;
}
