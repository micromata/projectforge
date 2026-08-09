import { describe, expect, it } from "vitest";
import type { FormatContext } from "@/lib/format";
import type { FilterElement } from "@/lib/rs/types";
import type { FilterValues } from "./filter-value";
import {
  HISTORY_FILTER_FIELDS,
  clearHistoryFilters,
  historyFilterActive,
  historyFilterGroupOf,
  mergeHistoryFilters,
  pickHistoryFilters,
  withoutHistoryFilters,
} from "./history-filter";
import { describeHistoryFilter } from "./history-filter-summary";

const berlin: FormatContext = { locale: "de-DE", timeZone: "Europe/Berlin" };

function element(id: string, filterType = "STRING"): FilterElement {
  return { id, label: id, filterType } as FilterElement;
}

/** A layout as the backend sends it: the three history fields plus the entity's own. */
const ELEMENTS = [
  element("titel"),
  element(HISTORY_FILTER_FIELDS.user, "OBJECT"),
  element(HISTORY_FILTER_FIELDS.interval, "TIMESTAMP"),
  element(HISTORY_FILTER_FIELDS.value),
  element("deleted", "BOOLEAN"),
];

describe("historyFilterGroupOf", () => {
  it("picks the three out of a layout", () => {
    const group = historyFilterGroupOf(ELEMENTS);
    expect(group?.user?.id).toBe(HISTORY_FILTER_FIELDS.user);
    expect(group?.interval?.id).toBe(HISTORY_FILTER_FIELDS.interval);
    expect(group?.value?.id).toBe(HISTORY_FILTER_FIELDS.value);
  });

  it("is null for an entity that carries none of them", () => {
    expect(historyFilterGroupOf([element("titel")])).toBeNull();
  });

  it("degrades to the fields that are there", () => {
    const group = historyFilterGroupOf([
      element("titel"),
      element(HISTORY_FILTER_FIELDS.interval, "TIMESTAMP"),
    ]);
    expect(group).not.toBeNull();
    expect(group?.interval).toBeDefined();
    expect(group?.user).toBeUndefined();
    expect(group?.value).toBeUndefined();
  });
});

describe("withoutHistoryFilters", () => {
  it("leaves the entity's own fields, in order", () => {
    expect(withoutHistoryFilters(ELEMENTS).map((e) => e.id)).toEqual([
      "titel",
      "deleted",
    ]);
  });
});

describe("historyFilterActive", () => {
  it("is false when nothing of the three narrows the list", () => {
    expect(historyFilterActive({})).toBe(false);
    expect(historyFilterActive({ titel: { value: "*a*" } })).toBe(false);
    // An empty value never gets stored, but a restored favorite may carry one.
    expect(historyFilterActive({ [HISTORY_FILTER_FIELDS.value]: {} })).toBe(
      false
    );
  });

  it("is true for any one of the three", () => {
    expect(
      historyFilterActive({ [HISTORY_FILTER_FIELDS.user]: { id: 42 } })
    ).toBe(true);
    expect(
      historyFilterActive({
        [HISTORY_FILTER_FIELDS.interval]: { from: "2026-07-15T08:00:00.000Z" },
      })
    ).toBe(true);
    expect(
      historyFilterActive({ [HISTORY_FILTER_FIELDS.value]: { value: "a" } })
    ).toBe(true);
  });
});

describe("pick / merge / clear", () => {
  const values: FilterValues = {
    titel: { value: "*a*" },
    [HISTORY_FILTER_FIELDS.user]: { id: 42, displayName: "Kai Reinhard" },
    [HISTORY_FILTER_FIELDS.value]: { value: "Titel" },
  };

  it("picks only the history fields", () => {
    expect(pickHistoryFilters(values)).toEqual({
      [HISTORY_FILTER_FIELDS.user]: { id: 42, displayName: "Kai Reinhard" },
      [HISTORY_FILTER_FIELDS.value]: { value: "Titel" },
    });
  });

  it("clears only the history fields", () => {
    expect(clearHistoryFilters(values)).toEqual({ titel: { value: "*a*" } });
  });

  it("does not mutate its input", () => {
    const before = JSON.stringify(values);
    clearHistoryFilters(values);
    mergeHistoryFilters(values, {});
    expect(JSON.stringify(values)).toBe(before);
  });

  it("replaces the three and keeps the rest", () => {
    const merged = mergeHistoryFilters(values, {
      [HISTORY_FILTER_FIELDS.interval]: { from: "2026-07-15T08:00:00.000Z" },
    });
    expect(merged).toEqual({
      titel: { value: "*a*" },
      [HISTORY_FILTER_FIELDS.interval]: { from: "2026-07-15T08:00:00.000Z" },
    });
  });

  it("drops values that would not narrow the list", () => {
    const merged = mergeHistoryFilters(values, {
      [HISTORY_FILTER_FIELDS.user]: {},
      [HISTORY_FILTER_FIELDS.value]: { value: "" },
    });
    expect(merged).toEqual({ titel: { value: "*a*" } });
  });
});

describe("describeHistoryFilter", () => {
  it("is empty when nothing is set", () => {
    expect(describeHistoryFilter({}, berlin)).toBe("");
  });

  it("names the user", () => {
    expect(
      describeHistoryFilter(
        {
          [HISTORY_FILTER_FIELDS.user]: { id: 42, displayName: "Kai Reinhard" },
        },
        berlin
      )
    ).toBe("Kai Reinhard");
  });

  it("falls back to the id when no name came along", () => {
    expect(
      describeHistoryFilter(
        { [HISTORY_FILTER_FIELDS.user]: { id: 42 } },
        berlin
      )
    ).toBe("#42");
  });

  it("renders the interval in the user's zone and locale", () => {
    const text = describeHistoryFilter(
      {
        [HISTORY_FILTER_FIELDS.interval]: {
          from: "2026-07-15T08:30:00.000Z",
          to: "2026-07-16T21:59:00.000Z",
        },
      },
      berlin
    );
    // 08:30Z is 10:30 in Berlin — the point of formatting through lib/format.ts.
    expect(text).toContain("10:30");
    expect(text).toContain("23:59");
    expect(text).toContain("–");
  });

  it("marks an open end with an ellipsis", () => {
    expect(
      describeHistoryFilter(
        {
          [HISTORY_FILTER_FIELDS.interval]: {
            from: "2026-07-15T08:30:00.000Z",
          },
        },
        berlin
      )
    ).toMatch(/ – …$/);
    expect(
      describeHistoryFilter(
        {
          [HISTORY_FILTER_FIELDS.interval]: { to: "2026-07-15T08:30:00.000Z" },
        },
        berlin
      )
    ).toMatch(/^… – /);
  });

  it("strips the wildcards off the free text", () => {
    expect(
      describeHistoryFilter(
        { [HISTORY_FILTER_FIELDS.value]: { value: "*Titel*" } },
        berlin
      )
    ).toBe("Titel");
  });

  it("joins all three in a stable order", () => {
    const text = describeHistoryFilter(
      {
        [HISTORY_FILTER_FIELDS.value]: { value: "Titel" },
        [HISTORY_FILTER_FIELDS.user]: { id: 42, displayName: "Kai Reinhard" },
        [HISTORY_FILTER_FIELDS.interval]: {
          from: "2026-07-15T08:30:00.000Z",
          to: "2026-07-16T21:59:00.000Z",
        },
      },
      berlin
    );
    expect(text.startsWith("Kai Reinhard, ")).toBe(true);
    expect(text.endsWith(", Titel")).toBe(true);
  });
});
