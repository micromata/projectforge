"use client";

import { useId, type ReactNode } from "react";
import { useTranslations } from "next-intl";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { DateInput } from "@/components/shared/date-input";
import { PersonSelect } from "@/components/shared/person-select";
import { SuggestInput } from "@/components/shared/suggest-input";
import { TimeInput } from "@/components/shared/time-input";
import { fetchLocationSuggestions } from "@/lib/rs/timesheet";
import { cn } from "@/lib/utils";
import { TaskKost2Picker } from "../task-kost2-picker";
import { BookDaysVacationSelect } from "./book-days-vacation-select";
import type {
  BookDaysVacation,
  BookDaysVacations,
  BookDaysValues,
} from "./types";

function LabeledField({
  id,
  label,
  className,
  children,
}: {
  id: string;
  label: string;
  className?: string;
  children: ReactNode;
}) {
  return (
    <div className={cn("flex flex-col gap-1.5", className)}>
      <Label htmlFor={id}>{label}</Label>
      {children}
    </div>
  );
}

/** The inputs of the multi-day booking: who, on what, which period and how long per day. */
export function BookDaysFields({
  values,
  onChange,
  vacations,
  onVacation,
  canChooseUser,
}: {
  values: BookDaysValues;
  onChange: (patch: Partial<BookDaysValues>) => void;
  vacations: BookDaysVacations | undefined;
  onVacation: (vacation: BookDaysVacation) => void;
  /** Booking for others (Orga, HR, …) — the calendar filter's "other users" right. */
  canChooseUser: boolean;
}) {
  const t = useTranslations();
  const id = useId();

  return (
    <div className="grid grid-cols-2 gap-x-3 gap-y-3">
      <LabeledField
        id={`${id}-user`}
        label={t("timesheet.user")}
        className="col-span-2"
      >
        <PersonSelect
          id={`${id}-user`}
          value={values.user}
          onChange={(user) => onChange({ user, vacationId: null })}
          required
          disabled={!canChooseUser}
        />
      </LabeledField>
      <TaskKost2Picker
        className="col-span-2"
        taskId={values.taskId}
        kost2Id={values.kost2Id}
        onTaskChange={(task) => onChange({ taskId: task?.id ?? null })}
        onKost2Change={(kost2Id) => onChange({ kost2Id })}
        required
        pickSingleKost2
      />
      <div className="col-span-2">
        <BookDaysVacationSelect
          id={`${id}-vacation`}
          vacations={vacations?.vacations ?? []}
          configured={vacations?.vacationBooking != null}
          value={values.vacationId}
          onSelect={onVacation}
        />
      </div>
      <LabeledField id={`${id}-start`} label={t("date.begin")}>
        <DateInput
          id={`${id}-start`}
          value={values.startDate}
          onChange={(startDate) => onChange({ startDate, vacationId: null })}
          required
        />
      </LabeledField>
      <LabeledField id={`${id}-end`} label={t("date.end")}>
        <DateInput
          id={`${id}-end`}
          value={values.endDate}
          defaultMonth={values.startDate}
          onChange={(endDate) => onChange({ endDate, vacationId: null })}
          required
        />
      </LabeledField>
      <LabeledField id={`${id}-time`} label={t("timesheet.startTime")}>
        <TimeInput
          id={`${id}-time`}
          value={values.startTime}
          onChange={(startTime) => onChange({ startTime })}
        />
      </LabeledField>
      <LabeledField
        id={`${id}-hours`}
        label={t("timesheet.bookDays.hoursPerDay")}
      >
        <Input
          id={`${id}-hours`}
          type="number"
          min={0.25}
          max={14}
          // A fifth of a 38.5 hours week is 7.7: any value, the backend rounds to 5 minutes.
          step="any"
          value={values.hoursPerDay ?? ""}
          onChange={(event) =>
            onChange({
              hoursPerDay:
                event.target.value === "" ? null : Number(event.target.value),
            })
          }
        />
      </LabeledField>
      <LabeledField
        id={`${id}-location`}
        label={t("timesheet.location")}
        className="col-span-2"
      >
        <SuggestInput
          id={`${id}-location`}
          value={values.location}
          onChange={(location) => onChange({ location })}
          suggest={fetchLocationSuggestions}
          queryKey={["timesheet", "location"]}
          minChars={0}
        />
      </LabeledField>
      <LabeledField
        id={`${id}-description`}
        label={t("description")}
        className="col-span-2"
      >
        <Textarea
          id={`${id}-description`}
          rows={3}
          value={values.description}
          onChange={(event) => onChange({ description: event.target.value })}
        />
      </LabeledField>
    </div>
  );
}
