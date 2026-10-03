import { describe, expect, it } from "vitest";
import { jsonChanges, parseJsonContainer } from "./json-diff";

describe("parseJsonContainer", () => {
  it("reads objects and arrays only", () => {
    expect(parseJsonContainer('{"a":1}')).toEqual({ a: 1 });
    expect(parseJsonContainer(" [1, 2] ")).toEqual([1, 2]);
    expect(parseJsonContainer("42")).toBeNull();
    expect(parseJsonContainer("plain text")).toBeNull();
    expect(parseJsonContainer("{broken")).toBeNull();
    expect(parseJsonContainer(null)).toBeNull();
  });
});

describe("jsonChanges", () => {
  const before = {
    version: 1,
    groups: [
      {
        key: "acme01",
        name: "ACME",
        customers: [4711, 4712],
        texts: ["ACME*"],
      },
      { key: "beta01", name: "Beta", customers: [1] },
    ],
  };

  it("matches array elements by key, names them by name and merges a scalar list", () => {
    const after = {
      version: 1,
      groups: [
        { key: "beta01", name: "Beta", customers: [1] },
        {
          key: "acme01",
          name: "ACME",
          customers: [4711, 4713],
          texts: ["ACME*"],
          remark: "Merger",
        },
      ],
    };
    expect(jsonChanges(before, after)).toEqual([
      {
        path: ["groups", "ACME", "customers"],
        removed: ["4712"],
        added: ["4713"],
      },
      { path: ["groups", "ACME", "remark"], removed: [], added: ["Merger"] },
    ]);
  });

  it("shows an added and a removed element as a whole", () => {
    const after = {
      version: 1,
      groups: [before.groups[0], { key: "gamma1", name: "Gamma" }],
    };
    const changes = jsonChanges(before, after);
    expect(changes).toContainEqual({
      path: ["groups", "Gamma"],
      removed: [],
      added: ['{"key":"gamma1","name":"Gamma"}'],
    });
    expect(changes).toContainEqual({
      path: ["groups", "Beta"],
      removed: ['{"key":"beta01","name":"Beta","customers":[1]}'],
      added: [],
    });
  });

  it("keeps a renamed element as the same one", () => {
    const after = structuredClone(before);
    after.groups[0].name = "ACME Corp";
    expect(jsonChanges(before, after)).toEqual([
      {
        path: ["groups", "ACME Corp", "name"],
        removed: ["ACME"],
        added: ["ACME Corp"],
      },
    ]);
  });

  it("reports a changed scalar with its old and new value", () => {
    expect(jsonChanges({ version: 1 }, { version: 2 })).toEqual([
      { path: ["version"], removed: ["1"], added: ["2"] },
    ]);
  });

  it("finds nothing in equal values", () => {
    expect(jsonChanges(before, structuredClone(before))).toEqual([]);
  });
});
