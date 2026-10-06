import type { EntityRef } from "@/components/shared/entity-autocomplete";

/** What the multi-day booking dialog collects. */
export interface BookDaysValues {
  user: EntityRef | null;
  taskId: number | null;
  kost2Id: number | null;
  /** ISO dates (yyyy-MM-dd), both inclusive. */
  startDate: string | null;
  endDate: string | null;
  /** Wall-clock start of every booked day, HH:mm. */
  startTime: string | null;
  hoursPerDay: number | null;
  location: string;
  description: string;
  /** Book only half the hours on the first / last day — taken over from a half-day vacation. */
  halfDayBegin: boolean;
  halfDayEnd: boolean;
  /** The vacation the period was taken from, if any. */
  vacationId: number | null;
}

/** Mirror of `TimesheetBookDaysRest.Vacation`: a vacation the period can be taken from. */
export interface BookDaysVacation {
  id: number;
  startDate: string;
  endDate: string;
  halfDayBegin: boolean;
  halfDayEnd: boolean;
  special: boolean;
  workingDays: number;
  status: "APPROVED" | "IN_PROGRESS" | "REJECTED";
}

/** Mirror of `TimesheetBookDaysRest.Vacations`; Spring omits an unset `vacationBooking` (`NON_NULL`). */
export interface BookDaysVacations {
  vacations: BookDaysVacation[];
  /**
   * The task and cost unit vacations are booked on, if projectforge.properties names them, and the
   * title the calendar shows for a sheet booked on them.
   */
  vacationBooking?: { taskId: number; kost2Id: number; title: string } | null;
}

/** Mirror of `TimesheetBookDaysRest.Request`. */
export interface BookDaysRequest {
  userId: number | null;
  taskId: number | null;
  kost2Id: number | null;
  location: string | null;
  description: string | null;
  startDate: string | null;
  endDate: string | null;
  startTime: string | null;
  hoursPerDay: number | null;
  halfDayBegin: boolean;
  halfDayEnd: boolean;
  dryRun: boolean;
}

/** `TimesheetDayBookingService.Status`. */
export type DayPlanStatus =
  | "BOOK"
  | "PARTIAL"
  | "WEEKEND"
  | "HOLIDAY"
  | "BOOKED";

/** One day of the period and what the booking does with it. */
export interface DayPlan {
  date: string;
  status: DayPlanStatus;
  /** Hours booked on this day; 0 for a skipped one. */
  hours: number;
  /** The holiday's name, if the day is one. */
  note?: string;
}

/** Mirror of `TimesheetBookDaysRest.Result`. */
export interface BookDaysResult {
  days: DayPlan[];
  /** The days booked (or, in a dry run, to be booked). */
  bookedCount: number;
}
