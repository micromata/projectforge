"use client";

import { useTranslations } from "next-intl";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Label } from "@/components/ui/label";
import { useFormatContext } from "@/hooks/use-format";
import { formatDateRange, formatNumber } from "@/lib/format";
import type { BookDaysVacation } from "./types";

/** Literal keys, so the i18n generator finds them (it exports only keys spelled out in the sources). */
const STATUS_KEYS = {
  APPROVED: "vacation.status.approved",
  IN_PROGRESS: "vacation.status.inProgress",
  REJECTED: "vacation.status.rejected",
} as const;

/**
 * Takes a current or past vacation of the user over: its period and half days are booked, on the vacation
 * task and cost unit configured in projectforge.properties. Hidden while the user has no vacation.
 */
export function BookDaysVacationSelect({
  id,
  vacations,
  configured,
  value,
  onSelect,
}: {
  id: string;
  vacations: BookDaysVacation[];
  /** Whether a vacation task and cost unit are configured, which a chosen vacation is booked on. */
  configured: boolean;
  value: number | null;
  onSelect: (vacation: BookDaysVacation) => void;
}) {
  const t = useTranslations();
  const ctx = useFormatContext();
  if (vacations.length === 0) return null;

  const labelOf = (vacation: BookDaysVacation) =>
    [
      formatDateRange(vacation.startDate, vacation.endDate, ctx),
      t("timesheet.bookDays.vacationDays", {
        arg0: formatNumber(vacation.workingDays, ctx),
      }),
      t(STATUS_KEYS[vacation.status]),
      vacation.special && t("vacation.special._"),
    ]
      .filter(Boolean)
      .join(" · ");

  return (
    <div className="flex flex-col gap-1.5">
      <Label htmlFor={id}>{t("timesheet.bookDays.vacation")}</Label>
      <Select
        value={value != null ? String(value) : ""}
        onValueChange={(selected) => {
          const vacation = vacations.find((v) => String(v.id) === selected);
          if (vacation) onSelect(vacation);
        }}
      >
        <SelectTrigger id={id} className="w-full">
          <SelectValue
            placeholder={t("timesheet.bookDays.vacationPlaceholder")}
          />
        </SelectTrigger>
        <SelectContent>
          {vacations.map((vacation) => (
            <SelectItem key={vacation.id} value={String(vacation.id)}>
              {labelOf(vacation)}
            </SelectItem>
          ))}
        </SelectContent>
      </Select>
      {value != null && (
        <p className="text-xs text-muted-foreground">
          {configured
            ? t("timesheet.bookDays.vacationHint")
            : t("timesheet.bookDays.vacationNotConfigured")}
        </p>
      )}
    </div>
  );
}
