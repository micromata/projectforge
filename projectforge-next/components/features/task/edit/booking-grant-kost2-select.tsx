"use client";

import { useTranslations } from "next-intl";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import type { FieldIds } from "@/components/shared/form/field-shell";
import type { TaskKost2 } from "@/lib/rs/task";

/** Marks "every cost unit of the task" in the select, which has no empty value. */
const ALL_KOST2 = "all";

/**
 * The cost unit a booking grant binds its user to, or none: every cost unit of the task.
 */
export function BookingGrantKost2Select({
  ids,
  kost2List,
  value,
  onChange,
}: {
  ids: FieldIds;
  kost2List: TaskKost2[];
  value: number | null;
  onChange: (value: number | null) => void;
}) {
  const t = useTranslations();
  return (
    <Select
      value={value != null ? String(value) : ALL_KOST2}
      onValueChange={(it) => onChange(it === ALL_KOST2 ? null : Number(it))}
    >
      <SelectTrigger
        id={ids.controlId}
        aria-labelledby={ids.labelId}
        className="w-full"
      >
        <SelectValue />
      </SelectTrigger>
      <SelectContent>
        <SelectItem value={ALL_KOST2}>
          {t("task.bookingGrant.kost2.all")}
        </SelectItem>
        {kost2List.map((kost2) => (
          <SelectItem key={kost2.id} value={String(kost2.id)}>
            {kost2.title}
          </SelectItem>
        ))}
      </SelectContent>
    </Select>
  );
}
