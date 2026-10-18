import { describe, expect, it } from "vitest";
import type { FormatContext } from "@/lib/format";
import type { FilterElement } from "@/lib/rs/types";
import { filterPillContent, prunedFilterValues } from "./filter-value";

const ctx: FormatContext = { locale: "de-DE", timeZone: "Europe/Berlin" };

const customers: FilterElement = {
  id: "customers",
  key: "customers",
  type: "FILTER_ELEMENT",
  filterType: "LIST",
  label: "Kunde",
  values: ["A", "B", "C", "D", "E"].map((name) => ({
    id: `k:${name}`,
    displayName: name,
  })),
};

describe("filterPillContent", () => {
  it("names the first three picks, counts the rest and lists all in the tooltip", () => {
    const content = filterPillContent(
      { values: ["k:A", "k:B", "k:C", "k:D", "k:E"] },
      customers,
      "Kunde",
      ctx
    );
    expect(content).toEqual({
      text: "A, B, C",
      more: 2,
      tooltip: "Kunde:\nA\nB\nC\nD\nE",
      tooltipPlain: true,
    });
  });

  it("lists the picks alphabetically, not in the order picked", () => {
    const content = filterPillContent(
      { values: ["k:D", "k:a", "k:C", "k:B"] },
      {
        ...customers,
        values: [...(customers.values ?? []), { id: "k:a", displayName: "a" }],
      },
      "Kunde",
      ctx
    );
    expect(content.text).toBe("a, B, C");
    expect(content.tooltip).toBe("Kunde:\na\nB\nC\nD");
  });

  it("shows a single pick as it is", () => {
    const content = filterPillContent(
      { values: ["k:B"] },
      customers,
      "Kunde",
      ctx
    );
    expect(content).toEqual({
      text: "B",
      more: 0,
      tooltip: "Kunde: B",
      tooltipPlain: false,
    });
  });

  it("shows a task without its ancestors, the path in the tooltip", () => {
    const task: FilterElement = {
      ...customers,
      id: "task",
      filterType: "OBJECT",
      values: undefined,
      autoCompletion: { type: "TASK" } as FilterElement["autoCompletion"],
    };
    const content = filterPillContent(
      { id: 7, displayName: "Root | Project | Task" },
      task,
      "Task",
      ctx
    );
    expect(content.text).toBe("Task");
    expect(content.tooltip).toBe("Task: Root | Project | Task");
  });
});

describe("prunedFilterValues", () => {
  const list = (id: string, choices: string[]): FilterElement => ({
    id,
    key: id,
    type: "FILTER_ELEMENT",
    filterType: "LIST",
    values: choices.map((choice) => ({ id: choice, displayName: choice })),
  });

  it("takes a value out that the field no longer offers", () => {
    const values = {
      campaign: { values: ["2"] },
      value: { values: ["Ja", "Nein"] },
    };
    expect(
      prunedFilterValues(
        values,
        [list("campaign", ["1", "2"]), list("value", ["Ja", "Nein"])],
        [list("campaign", ["1", "2"]), list("value", ["Nein", "Vielleicht"])]
      )
    ).toEqual({ campaign: { values: ["2"] }, value: { values: ["Nein"] } });
  });

  it("drops a field no longer offered, or with none of its values left", () => {
    const values = {
      value: { values: ["Ja"] },
      onlyDifferences: { value: "true" },
    };
    expect(
      prunedFilterValues(
        values,
        [
          list("value", ["Ja"]),
          {
            ...list("onlyDifferences", []),
            filterType: "BOOLEAN",
            values: undefined,
          },
        ],
        [list("value", ["Nein"])]
      )
    ).toEqual({});
  });

  it("keeps what didn't change, as the same object", () => {
    const values = {
      customers: { values: ["free text"] },
      period: { from: "2026-01-01" },
    };
    const elements = [list("customers", ["k:A"])];
    expect(
      prunedFilterValues(values, elements, [list("customers", ["k:A"])])
    ).toBe(values);
  });
});
