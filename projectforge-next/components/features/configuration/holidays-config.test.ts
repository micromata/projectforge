import { describe, expect, it } from "vitest";
import {
  EMPTY_HOLIDAY,
  HOLIDAYS_DEFAULTS,
  parseHolidaysConfig,
  serializeHolidaysConfig,
} from "./holidays-config";

const STORED =
  '[{"label":"Erster Mai","month":5,"dayOfMonth":1},{"label":"Dritter Oktober","month":10,"dayOfMonth":3},' +
  '{"id":"XMAS_EVE","workingDay":true,"workFraction":0.5},{"id":"NEW_YEARS_EVE","workingDay":true,"workFraction":0.5}]';

describe("parseHolidaysConfig", () => {
  it("reads own and pre-defined holidays", () => {
    const { entries, invalid } = parseHolidaysConfig(STORED);
    expect(invalid).toBe(false);
    expect(entries).toHaveLength(4);
    expect(entries[0]).toEqual({ ...EMPTY_HOLIDAY, label: "Erster Mai", month: 5, dayOfMonth: 1 });
    expect(entries[2]).toEqual({ ...EMPTY_HOLIDAY, id: "XMAS_EVE", workingDay: true, workFraction: 0.5 });
  });

  it("takes the defaults for an empty value and nothing for an invalid one", () => {
    expect(parseHolidaysConfig(null)).toEqual({ entries: HOLIDAYS_DEFAULTS, invalid: false });
    expect(parseHolidaysConfig("{}")).toEqual({ entries: [], invalid: true });
    expect(parseHolidaysConfig("[")).toEqual({ entries: [], invalid: true });
  });

  it("drops an unknown id, so the entry is an own holiday", () => {
    expect(parseHolidaysConfig('[{"id":"SYLVESTER"}]').entries[0].id).toBeNull();
  });
});

describe("serializeHolidaysConfig", () => {
  it("writes the stored value back unchanged", () => {
    expect(serializeHolidaysConfig(parseHolidaysConfig(STORED).entries)).toBe(STORED);
  });

  it("writes only the properties of the entry's kind", () => {
    expect(
      serializeHolidaysConfig([
        { ...EMPTY_HOLIDAY, id: "SHROVE_TUESDAY", ignore: true, workFraction: 0.5, month: 3 },
        { ...EMPTY_HOLIDAY, label: "Betriebsausflug ", year: 2027, month: 6, dayOfMonth: 4 },
      ])
    ).toBe(
      '[{"id":"SHROVE_TUESDAY","ignore":true,"workingDay":true},' +
        '{"label":"Betriebsausflug ","year":2027,"month":6,"dayOfMonth":4}]'
    );
  });
});
