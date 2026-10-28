/**
 * The JSON value of the configuration parameter `holidays`, mirroring `HolidaysConfig` / `ConfigureHoliday`
 * (projectforge-business): an array of entries, each either changing a pre-defined holiday (`id`) or adding an
 * own one (`label`, `month`, `dayOfMonth`, optional `year`). The backend validates it on saving.
 */
export const HOLIDAYS_PARAM = "holidays";

/** The pre-defined holidays (`HolidayDefinition`), in the order of the year. */
export const HOLIDAY_DEFINITIONS = [
  { id: "NEW_YEAR", i18nKey: "calendar.holiday.newYear" },
  { id: "SHROVE_MONDAY", i18nKey: "calendar.holiday.shroveMonday" },
  { id: "SHROVE_TUESDAY", i18nKey: "calendar.holiday.shroveTuesday" },
  { id: "ASH_WEDNESDAY", i18nKey: "calendar.holiday.ashWednesday" },
  { id: "PALM_SUNDAY", i18nKey: "calendar.holiday.palmSunday" },
  { id: "MAUNDY_THURSDAY", i18nKey: "calendar.holiday.maundyThursday" },
  { id: "GOOD_FRIDAY", i18nKey: "calendar.holiday.goodFriday" },
  { id: "EASTER_SUNDAY", i18nKey: "calendar.holiday.easterSunday" },
  { id: "EASTER_MONDAY", i18nKey: "calendar.holiday.easterMonday" },
  { id: "ASCENSION", i18nKey: "calendar.holiday.ascension" },
  { id: "WHIT_SUNDAY", i18nKey: "calendar.holiday.whitSunday" },
  { id: "WHIT_MONDAY", i18nKey: "calendar.holiday.whitMonday" },
  { id: "CORPUS_CHRISTI", i18nKey: "calendar.holiday.corpusChristi" },
  { id: "XMAS_EVE", i18nKey: "calendar.holiday.xmasEve" },
  { id: "FIRST_XMAS_DAY", i18nKey: "calendar.holiday.firstXmasDay" },
  { id: "SECOND_XMAS_DAY", i18nKey: "calendar.holiday.secondXmasDay" },
  { id: "NEW_YEARS_EVE", i18nKey: "calendar.holiday.newYearsEve" },
] as const;

export type HolidayId = (typeof HOLIDAY_DEFINITIONS)[number]["id"];

/**
 * An entry of the parameter. With `id`, it changes the pre-defined holiday: `ignore` removes it, `label`
 * replaces its name, `workFraction` makes it a partial working day (its working day flag is the
 * pre-defined one). Without, it adds the holiday `label` on `dayOfMonth`.`month` (every year or in `year`
 * only), a working day or not, with an optional `workFraction`.
 */
export interface HolidayEntry {
  id: HolidayId | null;
  label: string;
  year: number | null;
  month: number | null;
  dayOfMonth: number | null;
  workingDay: boolean;
  ignore: boolean;
  workFraction: number | null;
}

export const EMPTY_HOLIDAY: HolidayEntry = {
  id: null,
  label: "",
  year: null,
  month: null,
  dayOfMonth: null,
  workingDay: false,
  ignore: false,
  workFraction: null,
};

/** The backend's default (`ConfigurationParam.HOLIDAYS`), for a parameter not set yet. */
export const HOLIDAYS_DEFAULTS: HolidayEntry[] = [
  { ...EMPTY_HOLIDAY, id: "XMAS_EVE", workingDay: true, workFraction: 0.5 },
  { ...EMPTY_HOLIDAY, id: "NEW_YEARS_EVE", workingDay: true, workFraction: 0.5 },
];

const HOLIDAY_IDS = new Set<string>(HOLIDAY_DEFINITIONS.map((def) => def.id));

function numberOrNull(value: unknown): number | null {
  return typeof value === "number" && Number.isFinite(value) ? value : null;
}

/**
 * The entries of the stored value, the defaults for an empty one. `invalid` if the value isn't a JSON
 * array; no entries are shown then (the backend uses the pre-defined holidays only).
 */
export function parseHolidaysConfig(json: string | null | undefined): {
  entries: HolidayEntry[];
  invalid: boolean;
} {
  if (!json?.trim()) {
    return { entries: HOLIDAYS_DEFAULTS, invalid: false };
  }
  try {
    const value: unknown = JSON.parse(json);
    if (!Array.isArray(value)) {
      return { entries: [], invalid: true };
    }
    const entries = value
      .filter(
        (entry): entry is Record<string, unknown> =>
          typeof entry === "object" && entry !== null
      )
      .map(
        (entry): HolidayEntry => ({
          id:
            typeof entry.id === "string" && HOLIDAY_IDS.has(entry.id)
              ? (entry.id as HolidayId)
              : null,
          label: typeof entry.label === "string" ? entry.label : "",
          year: numberOrNull(entry.year),
          month: numberOrNull(entry.month),
          dayOfMonth: numberOrNull(entry.dayOfMonth),
          workingDay: entry.workingDay === true,
          ignore: entry.ignore === true,
          workFraction: numberOrNull(entry.workFraction),
        })
      );
    return { entries, invalid: false };
  } catch {
    return { entries: [], invalid: true };
  }
}

/**
 * The JSON value of the entries: only the properties an entry of its kind uses and only set ones, as the
 * backend writes it (`[{"id":"XMAS_EVE","workingDay":true,"workFraction":0.5}, …]`). Texts aren't trimmed
 * here (it's written on every keystroke), the backend trims them.
 */
export function serializeHolidaysConfig(entries: HolidayEntry[]): string {
  return JSON.stringify(
    entries.map((entry) => {
      const result: Record<string, unknown> = {};
      if (entry.id) {
        result.id = entry.id;
        if (entry.label.trim()) result.label = entry.label;
        if (entry.ignore) result.ignore = true;
        // The pre-defined flag applies, but a partial working day only makes sense on a working day.
        if (entry.workFraction != null) result.workingDay = true;
      } else {
        result.label = entry.label;
        if (entry.year != null) result.year = entry.year;
        result.month = entry.month;
        result.dayOfMonth = entry.dayOfMonth;
        if (entry.workingDay) result.workingDay = true;
      }
      if (entry.workFraction != null && !entry.ignore) {
        result.workFraction = entry.workFraction;
      }
      return result;
    })
  );
}
