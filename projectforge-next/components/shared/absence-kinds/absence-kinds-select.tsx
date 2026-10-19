"use client";

import { useId } from "react";
import { useTranslations } from "next-intl";
import { Label } from "@/components/ui/label";
import { FieldHint } from "@/components/shared/form/field-hint";
import { ToggleGroup, ToggleGroupItem } from "@/components/ui/toggle-group";
import {
  ABSENCE_KINDS,
  sortAbsenceKinds,
  type AbsenceKind,
} from "@/lib/rs/absence-kinds";
import { cn } from "@/lib/utils";

/** Each kind spelt out so the i18n key scanner sees every key (see NextI18nKeyScanner). */
const ABSENCE_KIND_KEYS: Record<AbsenceKind, string> = {
  VACATION: "vacation._",
  NOT_AVAILABLE: "availability.status.notAvailable",
  REMOTE_REACHABLE: "availability.status.remoteReachable",
  REACHABLE_IF_NEEDED: "availability.status.reachableIfNeeded",
};

interface AbsenceKindsSelectProps {
  /** The kinds chosen, in any order. */
  value: readonly AbsenceKind[];
  /** The kinds chosen, in their canonical order; never none: the last one can't be turned off. */
  onChange: (kinds: AbsenceKind[]) => void;
  /** Translated by the caller, so the key scanner sees the keys. */
  label: string;
  hint?: string;
  disabled?: boolean;
  className?: string;
}

/**
 * The kinds of absences shown (`AbsenceKind`): vacations and availability entries by their status, as a row of
 * toggles (several at once). The calendar, the team calendar and the team vacation calendar choose with it.
 */
export function AbsenceKindsSelect({
  value,
  onChange,
  label,
  hint,
  disabled = false,
  className,
}: AbsenceKindsSelectProps) {
  const t = useTranslations();
  const labelId = useId();
  return (
    <div className={cn("flex flex-col gap-1.5", className)}>
      <div className="flex items-center gap-1.5">
        <Label id={labelId} className="text-sm">
          {label}
        </Label>
        {hint && <FieldHint hint={hint} label={label} />}
      </div>
      <ToggleGroup
        type="multiple"
        variant="outline"
        size="sm"
        aria-labelledby={labelId}
        value={[...value]}
        disabled={disabled}
        onValueChange={(picked) => {
          // In the canonical order, whatever the click order.
          const next = sortAbsenceKinds(picked);
          if (next.length > 0) {
            onChange(next);
          }
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
