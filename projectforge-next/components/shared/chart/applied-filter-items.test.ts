import { describe, expect, it } from "vitest";
import type { FormatContext } from "@/lib/format";
import type { FilterElement, MagicFilter } from "@/lib/rs/types";
import { appliedFilterItems } from "./applied-filter-items";

const ctx: FormatContext = { locale: "de-DE", timeZone: "Europe/Berlin" };
const labels = { history: "Geändert", search: "Suchtext" };

function element(
  id: string,
  filterType: FilterElement["filterType"],
  extra: Partial<FilterElement> = {}
): FilterElement {
  return {
    id,
    key: id,
    type: "FILTER_ELEMENT",
    filterType,
    label: `label:${id}`,
    ...extra,
  };
}

const elements = [
  element("status", "LIST", {
    values: [{ id: "BEAUFTRAGT", displayName: "beauftragt" }],
  }),
  element("positionsStatus", "LIST"),
  element("periodOfPerformance", "DATE"),
  element("modifiedByUser", "OBJECT"),
  element("modifiedInterval", "TIMESTAMP"),
];

describe("appliedFilterItems", () => {
  it("lists the pills in field order, flagged as the chart used them", () => {
    const filter: MagicFilter = {
      entries: [
        { field: "paginationPageSize", value: { value: "50" } },
        {
          field: "periodOfPerformance",
          value: { from: "2026-01-01", to: "2026-12-31" },
        },
        { field: "positionsStatus", value: { values: ["LOI"] } },
        { field: "status", value: { values: ["BEAUFTRAGT"] } },
        { field: "unknownField", value: { value: "x" } },
      ],
      sortProperties: [],
      searchString: "ACME",
    };
    const items = appliedFilterItems(
      filter,
      elements,
      { ignored: ["positionsStatus"], replaced: ["periodOfPerformance"] },
      labels,
      ctx
    );
    expect(items.map((item) => [item.key, item.value, item.status])).toEqual([
      ["status", "beauftragt", "applied"],
      ["positionsStatus", "LOI", "ignored"],
      ["periodOfPerformance", "01.01.2026 – 31.12.2026", "replaced"],
      // Not offered as a field anymore, but still part of the query.
      ["unknownField", "x", "applied"],
      ["searchString", "ACME", "applied"],
    ]);
  });

  it("joins the history fields into one entry, as their pill does", () => {
    const filter: MagicFilter = {
      entries: [
        { field: "modifiedByUser", value: { id: 1, displayName: "Kai" } },
        { field: "modifiedInterval", value: { from: "2026-07-15T08:30:00Z" } },
      ],
      sortProperties: [],
    };
    const items = appliedFilterItems(filter, elements, undefined, labels, ctx);
    expect(items).toHaveLength(1);
    expect(items[0]).toMatchObject({
      key: "history",
      label: "Geändert",
      status: "applied",
    });
    expect(items[0].value).toMatch(/^Kai, 15\.07\.2026/);
  });

  it("is empty without a filter", () => {
    expect(
      appliedFilterItems(undefined, elements, undefined, labels, ctx)
    ).toEqual([]);
  });
});
