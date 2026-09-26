"use client";

import { useStore } from "@tanstack/react-form";
import { useTranslations } from "next-intl";
import Link from "next/link";
import { useEntityEditForm } from "@/components/shared/form/form-context";
import { cn } from "@/lib/utils";
import { LIQUIDITY_ROUTE } from "../liquidity.page";
import type { LiquidityValues } from "../liquidity-schema";

/**
 * The read-only pointer from a materialized occurrence back to its series — "Part of the series »X« —
 * Edit series", linking the focused series editor (`/liquidity/series/{seriesId}`). Shown *instead* of
 * the [RepeatFields] block whenever the entry already belongs to a series, so a single occurrence's form
 * only ever edits that one occurrence; the recurrence rule is changed in its own editor (see the plan's
 * copy-on-write model — no "this / all occurrences" prompt).
 *
 * Reachable from every occurrence, virtual or materialized, past or future — which is why the series
 * master is always reachable even when the whole (finite) series lies in the past.
 *
 * A custom field: the series id is not an editable value here, and the link is text, not an input.
 */
export function SeriesLink({ className }: { className?: string }) {
  const t = useTranslations();
  const form = useEntityEditForm();
  const seriesId = useStore(
    form.store,
    (s: unknown) => (s as FormState).values.seriesId
  );
  const subject = useStore(
    form.store,
    (s: unknown) => (s as FormState).values.subject
  );
  // A still-virtual occurrence being materialized has no id yet; an already-materialized one does.
  const isNew = useStore(
    form.store,
    (s: unknown) => (s as FormState).values.id == null
  );
  if (seriesId == null) return null;
  return (
    <div className={cn("flex flex-col gap-2", className)}>
      <p className="text-sm text-muted-foreground">
        {/* The generator renders positional `{0}` placeholders as `{arg0}` (see messages/generated.*.json). */}
        {t("plugins.liquidityplanning.series.partOf", { arg0: subject ?? "" })}{" "}
        {/* next/link prepends the app's basePath (/next) itself — see menu-url.ts. */}
        <Link
          href={`${LIQUIDITY_ROUTE}/series/${seriesId}`}
          className="font-medium text-primary underline underline-offset-2"
        >
          {t("plugins.liquidityplanning.series.editLink")}
        </Link>
      </p>
      {/* The copy-on-write hint: only while the occurrence is still virtual (first save freezes it). */}
      {isNew && (
        <p className="text-sm text-muted-foreground">
          {t("plugins.liquidityplanning.series.occurrenceHint")}
        </p>
      )}
    </div>
  );
}

/** The slice of the form store read here; the context is deliberately untyped (form-context). */
interface FormState {
  values: Pick<LiquidityValues, "id" | "seriesId" | "subject">;
}
