"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { Label } from "@/components/ui/label";
import { FieldHint } from "@/components/shared/form/field-hint";
import { ToggleGroup, ToggleGroupItem } from "@/components/ui/toggle-group";
import { ABSENCE_KINDS, type AbsenceKind } from "@/lib/rs/calendar-types";
import { cn } from "@/lib/utils";

/** Each kind spelt out so the i18n key scanner sees every key (see NextI18nKeyScanner). */
const ABSENCE_KIND_KEYS: Record<AbsenceKind, string> = {
  VACATION: "vacation._",
  NOT_AVAILABLE: "availability.status.notAvailable",
  REMOTE_REACHABLE: "availability.status.remoteReachable",
  REACHABLE_IF_NEEDED: "availability.status.reachableIfNeeded",
  REACHABLE_IF_URGENT: "availability.status.reachableIfUrgent",
};

interface CalendarAbsenceKindsProps {
  /** The stored choice; null for all kinds. */
  kinds: AbsenceKind[] | null | undefined;
  onChange: (kinds: AbsenceKind[]) => void;
}

/**
 * What the calendar shows of the absences of the vacation groups and users (`changeAbsenceKinds`): vacations and
 * availability entries by their status, as a row of toggles (several at once). The picks are held here, as the
 * vacation pickers do, because the server answers a change with `isFilterModified` only.
 */
export function CalendarAbsenceKinds({
  kinds,
  onChange,
}: CalendarAbsenceKindsProps) {
  const t = useTranslations();
  const [value, setValue] = useState<AbsenceKind[]>(() => [
    ...(kinds ?? ABSENCE_KINDS),
  ]);
  return (
    <div className="flex flex-col gap-1.5">
      <div className="flex items-center gap-1.5">
        <Label id="calendar-absence-kinds" className="text-sm">
          {t("calendar.filter.absenceKinds._")}
        </Label>
        <FieldHint
          hint={t("calendar.filter.absenceKinds.tooltip")}
          label={t("calendar.filter.absenceKinds._")}
        />
      </div>
      <ToggleGroup
        type="multiple"
        variant="outline"
        size="sm"
        aria-labelledby="calendar-absence-kinds"
        value={value}
        onValueChange={(picked) => {
          // In the canonical order, whatever the click order.
          const next = ABSENCE_KINDS.filter((kind) => picked.includes(kind));
          setValue(next);
          onChange(next);
        }}
        className="flex-wrap justify-start"
      >
        {ABSENCE_KINDS.map((kind) => (
          <ToggleGroupItem
            key={kind}
            value={kind}
            className={cn(
              "data-[state=on]:border-primary data-[state=on]:bg-primary data-[state=on]:text-primary-foreground",
              "hover:data-[state=on]:bg-primary/90"
            )}
          >
            {t(ABSENCE_KIND_KEYS[kind])}
          </ToggleGroupItem>
        ))}
      </ToggleGroup>
    </div>
  );
}
