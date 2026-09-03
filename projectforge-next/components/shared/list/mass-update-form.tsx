"use client";

import { useState, type ComponentType, type ReactNode } from "react";
import { useTranslations } from "next-intl";
import { useMutation } from "@tanstack/react-query";
import { toast } from "@/lib/toast";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import { ConfirmDialog } from "@/components/shared/confirm-dialog";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import { MarkdownText } from "@/components/shared/markdown-text";
import { Spinner } from "@/components/shared/spinner";
import {
  useSubmitShortcut,
  useSubmitShortcutHint,
} from "@/hooks/use-submit-shortcut";
import {
  cancelMultiSelection,
  massUpdate,
  type MassUpdateParameter,
  type MassUpdateResult,
  type MultiSelectMeta,
} from "@/lib/rs/multi-select";
import type { ValidationError } from "@/lib/rs/types";
import { MassUpdateField } from "./mass-update-field";
import { MassUpdateResultPanel } from "./mass-update-result-panel";

/**
 * The fields, the two buttons and what the run answered — everything of the mass update page that
 * exists only once its metadata has arrived (see [MassUpdatePage], which fetches it).
 */
export function MassUpdateForm({
  endpoint,
  meta,
  statisticsLine: StatisticsLine,
  selectedEntries,
  actions,
  onLeave,
}: {
  endpoint: string;
  meta: MultiSelectMeta;
  /** How the summary of the picked entries reads (see `MassUpdateDef.statisticsLine`). */
  statisticsLine?: ComponentType<{ statistics: unknown }>;
  /**
   * The collapsible list of the picked entries (see SelectedEntriesPanel).
   *
   * A slot rather than something built here, because it renders the *list's* columns: which those are
   * is the entity's business, and only the page that declares the list knows their types.
   */
  selectedEntries?: ReactNode;
  /**
   * A page-specific action beside the title, e.g. the incoming invoice's SEPA transfer export. A slot for
   * the same reason as [selectedEntries]: it acts on the entity, which this generic form does not know.
   */
  actions?: ReactNode;
  onLeave: () => void;
}) {
  const t = useTranslations();
  const shortcutHint = useSubmitShortcutHint();
  // One parameter per field, by field name — exactly the map the backend takes. A field whose
  // backend preset is "append" starts with the flag set, so text entered into it is added to the
  // existing value rather than overwriting it — the legacy `UILayout` form did this server side (see
  // `MassUpdateFieldMeta.appendPreset` / `createAndAddFields(showAppendOption = true)`).
  const [params, setParams] = useState<Record<string, MassUpdateParameter>>(
    () =>
      Object.fromEntries(
        meta.fields
          .filter((field) => field.appendPreset)
          .map((field) => [field.field, { append: true }])
      )
  );
  const [errors, setErrors] = useState<ValidationError[]>([]);
  const [result, setResult] = useState<MassUpdateResult | null>(null);
  const [confirming, setConfirming] = useState(false);

  const update = useMutation({
    mutationFn: () => massUpdate(endpoint, params),
    onSuccess: (outcome) => {
      if (outcome.kind === "validationErrors") {
        setErrors(outcome.validationErrors);
        setResult(null);
        return;
      }
      setErrors([]);
      setResult(outcome.result);
    },
    onError: (error) =>
      toast.error(error instanceof Error ? error.message : String(error)),
  });

  // Leaving drops the selection, so the next visit to the list starts clean rather than with what was
  // ticked an hour ago (the session context lives for 60 minutes).
  const leave = useMutation({
    mutationFn: () => cancelMultiSelection(endpoint),
    onSettled: onLeave,
  });

  // Return asks the question the button asks, not the write itself — every picked entry changes at
  // once, and a keystroke must not be the way around the confirmation.
  const canSubmit = !update.isPending && meta.selectedCount > 0;
  const onKeyDown = useSubmitShortcut(() => setConfirming(true), canSubmit);

  return (
    <div
      className="mx-auto w-full max-w-3xl space-y-4 p-4"
      onKeyDown={onKeyDown}
    >
      <div className="flex items-start justify-between gap-2">
        <div>
          <h1 className="text-lg font-bold tracking-tight">{meta.title}</h1>
          <p className="text-xs text-muted-foreground">
            {t("massUpdate.entriesFound", { arg0: meta.selectedCount })}
          </p>
        </div>
        {actions}
      </div>

      {/* What the picked entries add up to, by the entity's own statistics line — the same component the
          list shows above its table, so the numbers read identically on both pages. */}
      {StatisticsLine && meta.statisticsData != null && (
        <StatisticsLine statistics={meta.statisticsData} />
      )}

      {/* Above the fields, because it says what they are about to change — and closed, because the
          count and the sums answer the question for most visits. */}
      {selectedEntries}

      <div className="rounded-md border px-3">
        {meta.fields.map((field) => (
          <MassUpdateField
            key={field.field}
            meta={field}
            param={params[field.field] ?? {}}
            onChange={(param) =>
              setParams((previous) => ({ ...previous, [field.field]: param }))
            }
          />
        ))}
      </div>

      {meta.info && (
        <Alert>
          <AlertDescription>
            <MarkdownText text={meta.info} />
          </AlertDescription>
        </Alert>
      )}

      {errors.length > 0 && (
        <Alert variant="destructive">
          <AlertDescription>
            {errors.map((error) => error.message).join(" ")}
          </AlertDescription>
        </Alert>
      )}

      {result && <MassUpdateResultPanel result={result} />}

      <div className="flex items-center gap-2">
        <HintTooltip {...shortcutHint}>
          <Button
            type="button"
            disabled={!canSubmit}
            onClick={() => setConfirming(true)}
          >
            {update.isPending ? (
              <Spinner className="h-3.5 w-3.5 border-2" />
            ) : null}
            {t("save")}
          </Button>
        </HintTooltip>
        <Button type="button" variant="ghost" onClick={() => leave.mutate()}>
          {t("cancel")}
        </Button>
      </div>

      {/* Asked before the write, not after: it changes every picked entry at once and there is no undo
          beyond the Excel protocol. */}
      <ConfirmDialog
        open={confirming}
        onOpenChange={setConfirming}
        title={meta.title}
        description={t("massUpdate.confirmQuestion", {
          arg0: meta.selectedCount,
        })}
        confirmLabel={t("save")}
        onConfirm={() => {
          setConfirming(false);
          update.mutate();
        }}
      />
    </div>
  );
}
