/** The labels of the two comparison periods before a twelve month window, as shown instead of "previous year". */
export interface PreviousPeriodLabels {
  /** The window one year before the current one. */
  prevYear: string;
  /** The window two years before the current one. */
  prevPrevYear: string;
}

/**
 * The names of the two periods before the 12 months window that starts with [firstMonth] (`yyyy-MM`): the
 * calendar year (`2024`) if the window starts in January, else both years it spans (`2024/2025` for a window
 * 11/2024–10/2025). Without a valid month the labels stay empty.
 */
export function previousPeriodLabels(
  firstMonth: string | null | undefined
): PreviousPeriodLabels {
  const match = /^(\d{4})-(\d{2})/.exec(firstMonth ?? "");
  if (!match) {
    return { prevYear: "", prevPrevYear: "" };
  }
  const year = Number(match[1]);
  const january = Number(match[2]) === 1;
  const label = (yearsBack: number) =>
    january
      ? String(year - yearsBack)
      : `${year - yearsBack}/${year - yearsBack + 1}`;
  return { prevYear: label(1), prevPrevYear: label(2) };
}
