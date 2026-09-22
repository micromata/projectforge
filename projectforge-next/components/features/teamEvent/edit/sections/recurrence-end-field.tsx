"use client";

import { useTranslations } from "next-intl";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { DateInput } from "@/components/shared/date-input";
import type { EndMode, RecurrenceModel } from "../recurrence-model";

/** The three end modes, each key spelt out so the scanner sees it (see NextI18nKeyScanner). */
const END_OPTIONS: { value: EndMode; labelKey: string }[] = [
  { value: "NEVER", labelKey: "plugins.teamcal.event.recurrence.end.never" },
  { value: "COUNT", labelKey: "plugins.teamcal.event.recurrence.end.after" },
  { value: "UNTIL", labelKey: "plugins.teamcal.event.recurrence.end.onDate" },
];

interface Props {
  model: RecurrenceModel;
  update: (next: RecurrenceModel) => void;
}

/**
 * The "End" row of every frequency: Never (nothing), After [n] time(s) (`COUNT`) or On date (`UNTIL`).
 * The count `Input` or the `DateInput` follows the picked mode, matching the legacy `computeEnd.js`.
 */
export function RecurrenceEndField({ model, update }: Props) {
  const t = useTranslations();
  return (
    <div className="flex flex-wrap items-center gap-2">
      <Label className="font-normal">
        {t("plugins.teamcal.event.recurrence.end._")}
      </Label>
      <Select
        value={model.endMode}
        onValueChange={(v) => update({ ...model, endMode: v as EndMode })}
      >
        <SelectTrigger
          aria-label={t("plugins.teamcal.event.recurrence.end._")}
          className="w-full min-w-32"
        >
          <SelectValue />
        </SelectTrigger>
        <SelectContent>
          {END_OPTIONS.map((o) => (
            <SelectItem key={o.value} value={o.value}>
              {t(o.labelKey)}
            </SelectItem>
          ))}
        </SelectContent>
      </Select>
      {model.endMode === "COUNT" && (
        <>
          <Input
            type="number"
            min={1}
            aria-label={t("plugins.teamcal.event.recurrence.end.after")}
            className="w-20"
            value={String(model.count)}
            onChange={(e) =>
              update({ ...model, count: clampCount(e.target.value) })
            }
          />
          <span className="text-sm text-muted-foreground">
            {t("plugins.teamcal.event.recurrence.end.times")}
          </span>
        </>
      )}
      {model.endMode === "UNTIL" && (
        <DateInput
          value={model.until}
          aria-label={t("plugins.teamcal.event.recurrence.end.onDate")}
          onChange={(v) => update({ ...model, until: v })}
        />
      )}
    </div>
  );
}

/** A repeat count kept at 1 or more, so a `COUNT` never drops below a single occurrence. */
function clampCount(value: string): number {
  const n = Number(value);
  if (!Number.isFinite(n)) return 1;
  return Math.max(Math.trunc(n), 1);
}
