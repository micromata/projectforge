"use client";

import { useQuery } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import type { EntityRef } from "@/components/shared/entity-autocomplete";
import { Spinner } from "@/components/shared/spinner";
import { fetchBookDaysInitial } from "@/lib/rs/timesheet-book-days";
import { BookDaysForm } from "./book-days-form";

export interface BookDaysDialogProps {
  /** The user whose time sheets the calendar shows, else the logged-in user. */
  defaultUser: EntityRef | null;
  currentUser: EntityRef | null;
  canChooseUser: boolean;
  /** First day of the period the calendar shows (ISO date); the period starts at its first free day. */
  viewStart: string;
  /**
   * After a booking, with the user booked for and the first booked day (ISO) — the calendar refreshes,
   * shows their sheets and moves to the booked period.
   */
  onBooked: (userId: number | null, firstDay: string | null) => void;
  onClose: () => void;
}

/**
 * Books time sheets for a whole period at once — a vacation, or Orga booking parental leave or sick
 * days for someone absent. Weekends, holidays and days already booked are skipped.
 *
 * The form mounts once the defaults are in (first free day, the user's hours per day), so its state
 * starts from them instead of being patched after the first render.
 */
export function BookDaysDialog({
  viewStart,
  onClose,
  ...props
}: BookDaysDialogProps) {
  const t = useTranslations();
  const userId = props.defaultUser?.id ?? null;
  const initial = useQuery({
    queryKey: ["timesheet", "bookDays", "initial", userId, viewStart],
    queryFn: ({ signal }) => fetchBookDaysInitial(userId, viewStart, signal),
    // Booked days change with every booking: always ask again when the dialog opens.
    gcTime: 0,
  });

  return (
    <Dialog open onOpenChange={(open) => !open && onClose()}>
      <DialogContent className="max-h-[90vh] overflow-y-auto sm:max-w-4xl">
        <DialogHeader>
          <DialogTitle>{t("timesheet.bookDays.title")}</DialogTitle>
          <DialogDescription>{t("timesheet.bookDays.info")}</DialogDescription>
        </DialogHeader>
        {initial.data ? (
          <BookDaysForm {...props} initial={initial.data} onClose={onClose} />
        ) : initial.error ? (
          <p className="text-sm text-destructive">{initial.error.message}</p>
        ) : (
          <div className="flex justify-center p-6">
            <Spinner />
          </div>
        )}
      </DialogContent>
    </Dialog>
  );
}
