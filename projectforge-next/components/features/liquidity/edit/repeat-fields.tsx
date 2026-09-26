"use client";

import { useStore } from "@tanstack/react-form";
import { useTranslations } from "next-intl";
import { CheckboxField } from "@/components/shared/form/checkbox-field";
import { useEntityEditForm } from "@/components/shared/form/form-context";
import { NumberField } from "@/components/shared/form/number-field";
import { leafKeyOf } from "@/lib/leaf-key";
import { cn } from "@/lib/utils";
import type { LiquidityValues } from "../liquidity-schema";

/**
 * The "repeat" block that turns a *new* liquidity entry into a recurring series — the counterpart of a
 * calendar event's recurrence, offered right where the entry is created. On save the backend reads the
 * block and stores a `LiquiditySeriesDO` from the entry's template values (see LiquidityEntityRest); the
 * occurrences themselves stay virtual until one is touched.
 *
 * Only shown for a new, series-free entry — its section's `visible` gates that (see LIQUIDITY_PAGE). An
 * existing entry, or one being materialized out of a series (`seriesId` preset), shows the read-only
 * "part of series …" link instead ([SeriesLink]); a series can't spawn another series.
 *
 * A custom field because the interval and the installment count only make sense once the toggle is on,
 * and `repeat` is a nested block no single metadata-driven field could describe.
 */
export function RepeatFields({ className }: { className?: string }) {
  const t = useTranslations();
  const form = useEntityEditForm();
  const enabled = useStore(
    form.store,
    (s: unknown) => (s as FormState).values.repeat.enabled === true
  );
  return (
    <div className={cn("flex flex-col gap-3", className)}>
      <CheckboxField
        name="repeat.enabled"
        label={t("plugins.liquidityplanning.series.repeat.enable")}
      />
      {enabled && (
        <>
          {/* The one thing to know before turning it on: the occurrences are shown, not stored, and each is
              only frozen once touched — so the series stays editable in one place. */}
          <p className="text-sm text-muted-foreground">
            {t("plugins.liquidityplanning.series.repeat.info")}
          </p>
          <div className="flex flex-wrap items-start gap-4">
            {/* Every N months, anchored on the entry's date of payment; only monthly is offered for now. */}
            <NumberField
              name="repeat.intervalMonths"
              label={t("plugins.liquidityplanning.series.interval.months")}
              hint={t("plugins.liquidityplanning.series.interval.info")}
              metadataLess
              maxDigits={3}
            />
            {/* Empty = endless; a number caps the series at that many installments (LiquiditySeriesDO.count). */}
            <NumberField
              name="repeat.count"
              // `series.count` is both a label and the parent of `series.count.endless`, so the generator
              // exports the text as `series.count._` — resolve it through leafKeyOf like every other key.
              label={t(
                leafKeyOf("plugins.liquidityplanning.series.count", t.has)
              )}
              hint={t("plugins.liquidityplanning.series.count.endless")}
              metadataLess
              maxDigits={4}
            />
          </div>
        </>
      )}
    </div>
  );
}

/** The slice of the form store read here; the context is deliberately untyped (form-context). */
interface FormState {
  values: Pick<LiquidityValues, "repeat">;
}
