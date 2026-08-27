"use client";

import { useState } from "react";
import { useStore } from "@tanstack/react-form";
import { useTranslations } from "next-intl";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { FieldShell, useFieldIds } from "@/components/shared/form/field-shell";
import { useEntityEditForm } from "@/components/shared/form/form-context";
import { cn } from "@/lib/utils";
import {
  emptyRecurrence,
  parseRecurrence,
  recurrenceMode,
  serializeRecurrence,
  untilInstant,
  type RecurrenceMode,
  type RecurrenceModel,
} from "../recurrence-rrule";
import type { TeamEventEditValues } from "../team-event-edit-schema";
import { RecurrenceCustomized } from "./recurrence-customized";

/**
 * The top-level recurrence options, each with its i18n key spelt out in full so the key scanner sees it
 * (a `t(\`…${value}\`)` built at runtime would be invisible to it, see NextI18nKeyScanner). The four plain
 * frequencies write a bare `FREQ=…;INTERVAL=1`; "none" clears the rule; "customized" opens the panel.
 */
const MODES: { value: RecurrenceMode; labelKey: string }[] = [
  { value: "NONE", labelKey: "common.recurrence.frequency.none" },
  { value: "YEARLY", labelKey: "common.recurrence.frequency.yearly" },
  { value: "MONTHLY", labelKey: "common.recurrence.frequency.monthly" },
  { value: "WEEKLY", labelKey: "common.recurrence.frequency.weekly" },
  { value: "DAILY", labelKey: "common.recurrence.frequency.daily" },
  {
    value: "CUSTOMIZED",
    labelKey: "plugins.teamcal.event.recurrence.customized",
  },
];

/**
 * How the event repeats — the `recurrenceRule` field, mirroring the legacy `CalendarEventRecurrence`: a
 * frequency select whose plain entries write a bare rule and whose "customized" entry opens the detail
 * panel (interval, weekdays, end date), hand-built on the headless `rrule` package instead of the
 * Bootstrap-styled `react-rrule-generator` (see recurrence-rrule.ts and recurrence-customized.tsx).
 *
 * The stored rule string is the single source of truth for the rule itself; the mode is the one thing it
 * cannot always express (a customized rule can read back as a plain frequency), so the select holds it in
 * local state, seeded from the loaded rule. `recurrenceUntil` is kept in step with the rule's `UNTIL`, the
 * way `TeamEventDO.setRecurrence` derives it, so the denormalised column the backend queries on stays
 * consistent with the rule that actually bounds the series.
 */
export function RecurrenceSection({ className }: { className?: string }) {
  const t = useTranslations();
  const form = useEntityEditForm();
  const ids = useFieldIds();

  const rule = useStore(
    form.store,
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    (state: any) => (state.values as TeamEventEditValues).recurrenceRule
  );
  const model = parseRecurrence(rule);
  const [mode, setMode] = useState<RecurrenceMode>(() => recurrenceMode(model));

  const update = (next: RecurrenceModel) => {
    form.setFieldValue("recurrenceRule", serializeRecurrence(next) || null);
    form.setFieldValue("recurrenceUntil", untilInstant(next.until));
  };

  const onModeChange = (value: RecurrenceMode) => {
    setMode(value);
    if (value === "NONE") {
      update(emptyRecurrence());
    } else if (value === "CUSTOMIZED") {
      // Keep whatever rule is there; a rule without a frequency needs one to configure.
      if (!model.freq) update({ ...emptyRecurrence(), freq: "WEEKLY" });
    } else {
      // A plain frequency: a bare `FREQ=…;INTERVAL=1`, dropping any customized detail, as legacy did.
      update({ freq: value, interval: 1, byWeekday: [], until: null });
    }
  };

  return (
    <div className={cn("flex flex-col gap-4", className)}>
      <div className="grid grid-cols-1 gap-x-6 gap-y-4 md:grid-cols-3">
        <FieldShell
          name="recurrenceRule"
          label={t("plugins.teamcal.event.recurrence")}
          invalid={false}
          errors={[]}
          ids={ids}
        >
          <Select
            value={mode}
            onValueChange={(v) => onModeChange(v as RecurrenceMode)}
          >
            <SelectTrigger
              id={ids.controlId}
              aria-labelledby={ids.labelId}
              className="w-full"
            >
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              {MODES.map((m) => (
                <SelectItem key={m.value} value={m.value}>
                  {t(m.labelKey)}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </FieldShell>
      </div>

      {mode === "CUSTOMIZED" ? (
        <RecurrenceCustomized model={model} update={update} />
      ) : null}
    </div>
  );
}
