import { describe, expect, it } from "vitest";
import type { PropertyGroup, PropertyParam } from "@/lib/rs/properties-editor";
import { listEntryKeys, listsOf, nextListEntryKeys } from "./list-entries";
import { newParam, withAddedParams } from "./properties-editor-model";

const LIST = "projectforge.einvoice.seller.bankAccounts";

const param = (key: string, live = false): PropertyParam => ({
  ...newParam(key),
  known: true,
  status: "KNOWN",
  live,
});

const params = [
  param("projectforge.einvoice.seller.name"),
  param(`${LIST}[0].name`, true),
  param(`${LIST}[0].iban`, true),
  param(`${LIST}[1].name`, true),
  param(`${LIST}[1].iban`, true),
];

const groups: PropertyGroup[] = [
  {
    id: "einvoice",
    title: "E-invoice",
    expert: false,
    blocks: [{ description: "Bank accounts", params }],
  },
];

describe("list entries", () => {
  it("finds the lists of a block", () => {
    expect(listsOf(params)).toEqual([LIST]);
    expect(listsOf([param("a.b")])).toEqual([]);
  });

  it("adds the next index with every field", () => {
    expect(nextListEntryKeys(params, LIST)).toEqual([
      `${LIST}[2].name`,
      `${LIST}[2].iban`,
    ]);
  });

  it("finds the keys of one entry", () => {
    expect(listEntryKeys(params, LIST, 1)).toEqual([
      `${LIST}[1].name`,
      `${LIST}[1].iban`,
    ]);
    expect(listEntryKeys(params, LIST, 5)).toEqual([]);
  });

  it("places a new entry after the last one of its list", () => {
    const added = withAddedParams(groups, nextListEntryKeys(params, LIST));
    expect(added).toHaveLength(1);
    const keys = added[0].blocks[0].params.map((p) => p.key);
    expect(keys.slice(-2)).toEqual([`${LIST}[2].name`, `${LIST}[2].iban`]);
    const entry = added[0].blocks[0].params.at(-1);
    expect(entry?.live).toBe(true);
    expect(entry?.inFile).toBe(false);
  });

  it("puts other added keys into the group Other", () => {
    const added = withAddedParams(groups, ["spring.foo"]);
    expect(added.map((group) => group.id)).toEqual(["einvoice", "other"]);
  });
});
