import { describe, expect, it } from "vitest";
import type { FormatContext } from "@/lib/format";
import type { FilterElement } from "@/lib/rs/types";
import { filterPillContent } from "./filter-value";

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
