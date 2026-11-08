import { describe, expect, it } from "vitest";
import type { FilterElement } from "@/lib/rs/types";
import type { FilterValues } from "./filter-value";
import {
  foldedPinnedFilters,
  foldedPinnedFiltersLabel,
} from "./pinned-filter-group";

function element(id: string): FilterElement {
  return {
    id,
    label: id,
    filterType: "LIST",
    defaultFilter: true,
  } as FilterElement;
}

const ELEMENTS = [
  element("businessUnits"),
  element("customers"),
  element("projects"),
  element("titel"),
];

const ids = (elements: FilterElement[]) => elements.map((e) => e.id);
const picked: FilterValues = { customers: { values: ["1"] } };

describe("foldedPinnedFilters", () => {
  it("folds all three while empty", () => {
    expect(ids(foldedPinnedFilters(ELEMENTS, {}, []))).toEqual([
      "businessUnits",
      "customers",
      "projects",
    ]);
  });

  it("leaves a field with a value out", () => {
    expect(ids(foldedPinnedFilters(ELEMENTS, picked, []))).toEqual([
      "businessUnits",
      "projects",
    ]);
  });

  it("doesn't fold a single empty one", () => {
    const values = { ...picked, projects: { values: ["2"] } };
    expect(foldedPinnedFilters(ELEMENTS, values, [])).toEqual([]);
  });

  it("keeps the field being picked or edited on the row", () => {
    expect(ids(foldedPinnedFilters(ELEMENTS, {}, [null, "projects"]))).toEqual([
      "businessUnits",
      "customers",
    ]);
  });

  it("folds customer and project of a list without business units", () => {
    expect(ids(foldedPinnedFilters(ELEMENTS.slice(1), {}, []))).toEqual([
      "customers",
      "projects",
    ]);
  });

  it("folds nothing of a list without the group", () => {
    expect(foldedPinnedFilters([element("titel")], {}, [])).toEqual([]);
  });
});

describe("foldedPinnedFiltersLabel", () => {
  it("joins the labels", () => {
    expect(
      foldedPinnedFiltersLabel(foldedPinnedFilters(ELEMENTS, picked, []))
    ).toBe("businessUnits / projects");
  });
});
