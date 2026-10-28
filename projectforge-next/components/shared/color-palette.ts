/**
 * The ready-made colours of the colour picker, the usual choice. These are colour **data** stored per
 * calendar (`CalendarStyle.bgColor`) or business unit, not theme tokens — the same kind of value
 * FullCalendar paints an event with — so each swatch is rendered from its hex directly.
 *
 * One column per hue (teal, blue, indigo, purple, magenta, red, orange, yellow, green, brown, grey), one row
 * per shade from light to dark. The third row holds the brand palette and the former presets, so colours
 * chosen before are still found as a swatch.
 */
export const PRESET_ROWS = [
  [
    "#80cdd1",
    "#90caf9",
    "#9fa8da",
    "#ce93d8",
    "#f48fb1",
    "#ef9a9a",
    "#ffcc80",
    "#fff59d",
    "#c5e1a5",
    "#d7ccc8",
    "#cfd8dc",
  ],
  [
    "#4db6bc",
    "#42a5f5",
    "#5c6bc0",
    "#ab47bc",
    "#f06292",
    "#ef5350",
    "#ffb74d",
    "#ffe082",
    "#9ccc65",
    "#a1887f",
    "#90a4ae",
  ],
  [
    "#009ba3",
    "#1e88e5",
    "#3f51b5",
    "#9c27b0",
    "#e10057",
    "#e53935",
    "#ff9d01",
    "#ffcb00",
    "#75be01",
    "#795548",
    "#607d8b",
  ],
  [
    "#007480",
    "#1565c0",
    "#283593",
    "#6a1b9a",
    "#a70836",
    "#b71c1c",
    "#e65100",
    "#c79100",
    "#32912e",
    "#4e342e",
    "#37474f",
  ],
] as const;

/** The presets row by row, as the picker lays them out (11 per row). */
export const PRESET_COLORS: readonly string[] = PRESET_ROWS.flat();
