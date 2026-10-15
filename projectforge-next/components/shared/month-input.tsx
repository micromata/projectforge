"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import {
  ArrowLeft01Icon,
  ArrowRight01Icon,
  Calendar01Icon,
} from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from "@/components/ui/popover";
import { useFormatContext } from "@/hooks/use-format";
import { todayIso } from "@/lib/date-parse";
import { endOfMonth, isoOfParts, partsOf } from "@/lib/date-period-math";
import { formatMonthShortName, formatYearMonth } from "@/lib/format";
import { cn } from "@/lib/utils";

const MONTHS = Array.from({ length: 12 }, (_, i) => i + 1);

/**
 * A calendar month as the one value it is: a button naming it ("März 2026") that opens a year with its
 * twelve months. For a value the backend reads in whole months (an accounting record's booking period), where
 * a day field would pretend a precision the filter does not have.
 *
 * The value stays an ISO date, so it travels as every other date bound does: the first day of the month for
 * the begin of a range, the last day for its end ([bound]). Any day of a month selects that month.
 */
export function MonthInput({
  value,
  onChange,
  bound,
  defaultMonth,
  className,
  "aria-label": ariaLabel,
}: {
  value: string | null | undefined;
  onChange: (value: string | null) => void;
  /** Which day of the picked month the value becomes: its first (`begin`) or its last (`end`). */
  bound: "begin" | "end";
  /** Where an empty field opens, as `yyyy-MM-dd` — the other end of the range, once that is given. */
  defaultMonth?: string | null;
  className?: string;
  "aria-label": string;
}) {
  const t = useTranslations();
  const ctx = useFormatContext();
  const [open, setOpen] = useState(false);
  const selected = value ? partsOf(value) : null;
  const [year, setYear] = useState(
    () => partsOf(value ?? defaultMonth ?? todayIso()).year
  );

  // Opening lands on the year of the value (or of the other bound), not wherever it was browsed to last.
  const onOpenChange = (next: boolean) => {
    if (next) setYear(partsOf(value ?? defaultMonth ?? todayIso()).year);
    setOpen(next);
  };

  const pick = (month: number | null) => {
    const first = month == null ? null : isoOfParts(year, month, 1);
    onChange(first && bound === "end" ? endOfMonth(first) : first);
    setOpen(false);
  };

  const text = formatYearMonth(value, ctx);
  return (
    <Popover open={open} onOpenChange={onOpenChange}>
      <PopoverTrigger asChild>
        <Button
          type="button"
          variant="outline"
          aria-label={text ? `${ariaLabel}: ${text}` : ariaLabel}
          className={cn(
            "h-7 w-full justify-between px-2.5 font-normal tabular-nums",
            !text && "text-muted-foreground",
            className
          )}
        >
          <span className="truncate">{text || "…"}</span>
          <HugeiconsIcon icon={Calendar01Icon} size={14} />
        </Button>
      </PopoverTrigger>
      <PopoverContent align="start" className="w-60 p-2" aria-label={ariaLabel}>
        <div className="mb-2 flex items-center justify-between">
          <Button
            type="button"
            variant="ghost"
            size="icon"
            className="size-7"
            aria-label={t("calendar.previousYear")}
            onClick={() => setYear((y) => y - 1)}
          >
            <HugeiconsIcon icon={ArrowLeft01Icon} size={14} />
          </Button>
          <span className="text-sm font-medium tabular-nums">{year}</span>
          <Button
            type="button"
            variant="ghost"
            size="icon"
            className="size-7"
            aria-label={t("calendar.nextYear")}
            onClick={() => setYear((y) => y + 1)}
          >
            <HugeiconsIcon icon={ArrowRight01Icon} size={14} />
          </Button>
        </div>
        <div className="grid grid-cols-4 gap-1">
          {MONTHS.map((month) => {
            const isSelected =
              selected?.year === year && selected.month === month;
            return (
              <Button
                key={month}
                type="button"
                size="sm"
                variant={isSelected ? "default" : "ghost"}
                aria-pressed={isSelected}
                className="text-xs"
                onClick={() => pick(month)}
              >
                {formatMonthShortName(month, ctx)}
              </Button>
            );
          })}
        </div>
        <div className="mt-2 border-t pt-2">
          <Button
            type="button"
            variant="ghost"
            size="sm"
            className="w-full text-xs"
            disabled={!value}
            onClick={() => pick(null)}
          >
            {t("reset")}
          </Button>
        </div>
      </PopoverContent>
    </Popover>
  );
}
