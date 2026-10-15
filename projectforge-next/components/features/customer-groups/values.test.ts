import { describe, expect, it } from "vitest";
import { customerGroupsSchema } from "./schema";
import {
  isValidTextMember,
  newKey,
  toFormValues,
  toPayload,
  usedKeys,
} from "./values";

describe("isValidTextMember", () => {
  it("accepts names and leading or trailing wildcards, as TextPattern.of does", () => {
    for (const text of [
      "ACME",
      " ACME, Inc. ",
      "ACME*",
      "*Logistics",
      "*AC*",
      "100% Media",
    ])
      expect(isValidTextMember(text), text).toBe(true);
  });
  it("refuses what the server refuses", () => {
    for (const text of [
      "",
      "*",
      "**",
      "A",
      "*A*",
      "AC*ME",
      "AC%*",
      "*AC_ME",
      "* ACME",
    ])
      expect(isValidTextMember(text), text).toBe(false);
  });
});

describe("newKey", () => {
  it("has the server's format and avoids the keys in use", () => {
    const used = new Set(["abcdef"]);
    const key = newKey(used);
    expect(key).toMatch(/^[a-z0-9]{6}$/);
    expect(used.has(key)).toBe(false);
  });
});

describe("toFormValues", () => {
  it("fills in what Spring omitted and keys every row", () => {
    const values = toFormValues({
      groups: [{ key: "acmeg1", name: "ACME" }, { name: "New" }],
      businessUnits: [{ key: "logbu1", groups: ["acmeg1"] }],
    });
    expect(values.groups[0]).toEqual({
      key: "acmeg1",
      name: "ACME",
      customers: [],
      texts: [],
    });
    expect(values.groups[1].key).toMatch(/^[a-z0-9]{6}$/);
    expect(values.businessUnits[0]).toMatchObject({
      name: "",
      groups: ["acmeg1"],
    });
    expect(values.lastUpdate).toBeNull();
    expect(usedKeys(values).size).toBe(3);
  });
});

describe("toFormValues with a format", () => {
  it("sorts the groups and business units by name", () => {
    const values = toFormValues(
      {
        groups: [
          { key: "g10000", name: "Kunde 10" },
          { key: "g20000", name: "acme" },
          { key: "g30000", name: "Kunde 9" },
        ],
        businessUnits: [
          { key: "b10000", name: "Retail" },
          { key: "b20000", name: "Logistik" },
        ],
      },
      { locale: "de-DE", timeZone: "Europe/Berlin" }
    );
    expect(values.groups.map((group) => group.name)).toEqual([
      "acme",
      "Kunde 9",
      "Kunde 10",
    ]);
    expect(values.businessUnits.map((bu) => bu.name)).toEqual([
      "Logistik",
      "Retail",
    ]);
  });
});

describe("toPayload", () => {
  it("drops a business unit's reference to a group removed in the form", () => {
    const values = toFormValues({
      groups: [{ key: "acmeg1", name: "ACME" }],
      businessUnits: [
        { key: "logbu1", name: "L", groups: ["acmeg1", "gone12"] },
      ],
      lastUpdate: 42,
    });
    expect(toPayload(values).businessUnits[0].groups).toEqual(["acmeg1"]);
    expect(toPayload(values).lastUpdate).toBe(42);
  });
});

describe("customerGroupsSchema", () => {
  it("requires names and valid texts", () => {
    const values = toFormValues({
      groups: [{ key: "aaaa", name: " ", texts: ["A*B"] }],
    });
    const result = customerGroupsSchema.safeParse(values);
    expect(result.success).toBe(false);
    const paths = result.error?.issues.map((issue) => issue.path.join("."));
    expect(paths).toEqual(["groups.0.name", "groups.0.texts"]);
  });
  it("accepts a valid config", () => {
    const values = toFormValues({
      groups: [
        {
          key: "aaaa",
          name: "ACME",
          texts: ["ACME*"],
          customers: [{ id: 1, displayName: "ACME AG" }],
        },
      ],
      businessUnits: [{ key: "bbbb", name: "BU", groups: ["aaaa"] }],
    });
    expect(customerGroupsSchema.safeParse(values).success).toBe(true);
  });
});
