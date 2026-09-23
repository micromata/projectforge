/** Contract of org.projectforge.rest.PersonalStatisticsRest. */

/** One day of the discipline chart: cumulative planned ("soll") vs. booked ("ist") working hours. */
export interface WorkingHoursPoint {
  date: string;
  soll: number;
  ist: number;
  /** So a point is a plain record recharts (and DisciplineChart) can index by series key. */
  [key: string]: string | number;
}

/** One day of the booking-latency chart: the goal ("plan") vs. the actual average ("actual"), in days. */
export interface BookingLatencyPoint {
  date: string;
  plan: number;
  actual: number;
  /** So a point is a plain record recharts (and DisciplineChart) can index by series key. */
  [key: string]: string | number;
}

/** Key figures repeated in the two chart legends. */
export interface PersonalStatisticsSummary {
  planWorkingHours: number;
  actualWorkingHours: number;
  averageBookingLatency: number;
  plannedBookingLatency: number;
}

/** Full response: PersonalStatisticsRest.Statistics. */
export interface PersonalStatistics {
  lastNDays: number;
  workingHours: WorkingHoursPoint[];
  bookingLatency: BookingLatencyPoint[];
  summary: PersonalStatisticsSummary;
}
