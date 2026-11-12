/** The grid of the times of time sheets in minutes the user chose (5, 10, 15, 20 or 30), 5 for a server that sends none. */
export function timeGridOf(minutes: number | null | undefined): number {
  return minutes != null &&
    Number.isInteger(minutes) &&
    minutes >= 5 &&
    minutes <= 30 &&
    60 % minutes === 0
    ? minutes
    : 5;
}

const MINUTE_MS = 60_000;

/** [time] (ms since 1970) rounded down to the grid [step] (minutes); zones with whole-hour offsets keep the grid. */
export function floorToGrid(time: number, step: number): number {
  const stepMs = step * MINUTE_MS;
  const date = new Date(time);
  // Offsets of half an hour or a quarter exist (India, Nepal): the grid is that of the wall clock, not of UTC.
  const offsetMs = -date.getTimezoneOffset() * MINUTE_MS;
  return Math.floor((time + offsetMs) / stepMs) * stepMs - offsetMs;
}

/** [time] rounded up to the grid [step] (minutes). */
export function ceilToGrid(time: number, step: number): number {
  const floor = floorToGrid(time, step);
  return floor === time ? floor : floor + step * MINUTE_MS;
}
