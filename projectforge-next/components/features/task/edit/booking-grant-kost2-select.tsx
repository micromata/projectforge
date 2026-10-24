"use client";

import { useTranslations } from "next-intl";
import { ValueCombobox } from "@/components/shared/value-combobox";
import type { TaskKost2 } from "@/lib/rs/task";

/**
 * The cost units a task of a booking grant may be booked on, any number; none picked means every cost unit of
 * the task.
 */
export function BookingGrantKost2Select({
  label,
  kost2List,
  value,
  onChange,
}: {
  /** Accessible name, naming the task of the row. */
  label: string;
  kost2List: TaskKost2[];
  value: number[];
  onChange: (value: number[]) => void;
}) {
  const t = useTranslations();
  return (
    <ValueCombobox
      aria-label={label}
      multi
      options={kost2List.map((kost2) => ({
        value: String(kost2.id),
        label: kost2.title,
      }))}
      selected={value.map(String)}
      onChange={(values) => onChange(values.map(Number))}
      placeholder={t("task.bookingGrant.kost2.all")}
      className="w-full"
    />
  );
}
