"use client";

import { useState } from "react";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import { Button } from "@/components/ui/button";
import { DialogFooter } from "@/components/ui/dialog";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import { Spinner } from "@/components/shared/spinner";
import {
  useSubmitShortcut,
  useSubmitShortcutHint,
} from "@/hooks/use-submit-shortcut";
import {
  fetchBookDaysInitial,
  fetchBookDaysVacations,
  type BookDaysInitial,
} from "@/lib/rs/timesheet-book-days";
import { toast } from "@/lib/toast";
import type { TimesheetDetail } from "../types";
import type { BookDaysDialogProps } from "./book-days-dialog";
import { BookDaysFields } from "./book-days-fields";
import { BookDaysPreview } from "./book-days-preview";
import { BookDaysTemplates } from "./book-days-templates";
import type { BookDaysVacation, BookDaysValues } from "./types";
import { useBookDays } from "./use-book-days";

/** The statuses of a day that gets a time sheet. */
const BOOKING_STATUS = { BOOK: true, PARTIAL: true };

/** The dialog's inputs, its preview and its buttons, starting from the server's defaults. */
export function BookDaysForm({
  defaultUser,
  canChooseUser,
  initial,
  onBooked,
  onClose,
}: Omit<BookDaysDialogProps, "viewStart"> & { initial: BookDaysInitial }) {
  const t = useTranslations();
  const queryClient = useQueryClient();
  const shortcutHint = useSubmitShortcutHint();
  const [values, setValues] = useState<BookDaysValues>({
    user: defaultUser,
    taskId: null,
    kost2Id: null,
    startDate: initial.startDate,
    endDate: initial.startDate,
    startTime: "09:00",
    hoursPerDay: initial.hoursPerDay,
    location: "",
    description: "",
    halfDayBegin: false,
    halfDayEnd: false,
    vacationId: null,
  });
  const userId = values.user?.id ?? null;
  const vacations = useQuery({
    queryKey: ["timesheet", "bookDays", "vacations", userId],
    queryFn: ({ signal }) => fetchBookDaysVacations(userId, signal),
  });

  const onChange = (patch: Partial<BookDaysValues>) => {
    setValues((prev) => {
      const next = { ...prev, ...patch };
      // Half days belong to the vacation the period was taken from; a hand-edited period drops them.
      if (next.vacationId == null) {
        next.halfDayBegin = false;
        next.halfDayEnd = false;
      }
      return next;
    });
    // Another user works other hours (part time): take over theirs, the period stays as it is.
    if (patch.user !== undefined && patch.user?.id !== userId) {
      const id = patch.user?.id ?? null;
      void fetchBookDaysInitial(id, values.startDate ?? initial.startDate)
        .then((other) => onChange({ hoursPerDay: other.hoursPerDay }))
        .catch(() => undefined);
    }
  };
  const applyVacation = (vacation: BookDaysVacation) => {
    const booking = vacations.data?.vacationBooking;
    const text = t("timesheet.bookDays.vacationDescription");
    // No text if the calendar title of the vacation task says so already: it would show twice.
    const named = booking?.title.toLowerCase().includes(text.toLowerCase());
    onChange({
      vacationId: vacation.id,
      startDate: vacation.startDate,
      endDate: vacation.endDate,
      halfDayBegin: vacation.halfDayBegin,
      halfDayEnd: vacation.halfDayEnd,
      ...(booking && { taskId: booking.taskId, kost2Id: booking.kost2Id }),
      description: named ? "" : text,
    });
  };
  const applyTemplate = (template: TimesheetDetail) =>
    onChange({
      taskId: template.task?.id ?? null,
      kost2Id: template.kost2?.id ?? null,
      location: template.location ?? "",
      description: template.description ?? "",
    });

  const plan = useBookDays(
    values,
    (result) => {
      toast.success(
        t("timesheet.bookDays.success", { arg0: result.bookedCount })
      );
      void queryClient.invalidateQueries({ queryKey: ["timesheet", "recent"] });
      const first = result.days.find((day) => day.status in BOOKING_STATUS);
      onBooked(userId, first?.date ?? null);
      onClose();
    },
    (message) => toast.error(message)
  );
  const canSubmit =
    plan.bookCount > 0 &&
    values.user != null &&
    values.taskId != null &&
    !plan.booking.isPending;
  const submit = () => plan.booking.mutate();
  const onKeyDown = useSubmitShortcut(submit, canSubmit);

  return (
    <div onKeyDown={onKeyDown} className="flex flex-col gap-4">
      <div className="grid gap-4 md:grid-cols-[3fr_2fr]">
        <div className="flex flex-col gap-3">
          <BookDaysTemplates values={values} onApply={applyTemplate} />
          <BookDaysFields
            values={values}
            onChange={onChange}
            vacations={vacations.data}
            onVacation={applyVacation}
            canChooseUser={canChooseUser}
          />
        </div>
        <BookDaysPreview
          days={plan.days}
          loading={plan.loading}
          error={plan.error}
        />
      </div>
      <DialogFooter>
        <Button type="button" variant="outline" onClick={onClose}>
          {t("cancel")}
        </Button>
        <HintTooltip {...shortcutHint}>
          <Button type="button" onClick={submit} disabled={!canSubmit}>
            {plan.booking.isPending && <Spinner className="h-4 w-4 border-2" />}
            {t("timesheet.bookDays.submit", { arg0: plan.bookCount })}
          </Button>
        </HintTooltip>
      </DialogFooter>
    </div>
  );
}
