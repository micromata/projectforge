import { describe, expect, it } from "vitest";
import { mergeRights } from "./use-user-rights";
import type { UserRightInfo } from "@/lib/rs/user";

const SELECT: UserRightInfo = {
  rightId: "FIBU_EINGANGSRECHNUNGEN",
  label: "Incoming invoices",
  booleanType: false,
  values: [
    { value: "READONLY", label: "read-only" },
    { value: "READWRITE", label: "read/write" },
  ],
  value: "READONLY",
};

const BOOLEAN: UserRightInfo = {
  rightId: "HR_VACATION",
  label: "Vacation",
  booleanType: true,
  values: [],
  booleanValue: false,
};

describe("mergeRights", () => {
  it("keeps what the form holds over a cached answer of earlier groups", () => {
    const merged = mergeRights(
      [SELECT, BOOLEAN],
      [
        {
          rightId: "FIBU_EINGANGSRECHNUNGEN",
          value: "READWRITE",
          booleanValue: null,
        },
        { rightId: "HR_VACATION", value: null, booleanValue: true },
      ]
    );
    expect(merged).toEqual([
      {
        rightId: "FIBU_EINGANGSRECHNUNGEN",
        value: "READWRITE",
        booleanValue: null,
      },
      { rightId: "HR_VACATION", value: null, booleanValue: true },
    ]);
  });

  it("takes the answered value of a right the form doesn't hold yet", () => {
    expect(mergeRights([SELECT, BOOLEAN], [])).toEqual([
      {
        rightId: "FIBU_EINGANGSRECHNUNGEN",
        value: "READONLY",
        booleanValue: null,
      },
      { rightId: "HR_VACATION", value: null, booleanValue: false },
    ]);
  });

  it("drops a held value the right no longer offers, and rights no longer answered", () => {
    const merged = mergeRights(
      [{ ...SELECT, values: [{ value: "READONLY", label: "read-only" }] }],
      [
        {
          rightId: "FIBU_EINGANGSRECHNUNGEN",
          value: "READWRITE",
          booleanValue: null,
        },
        { rightId: "HR_VACATION", value: null, booleanValue: true },
      ]
    );
    expect(merged).toEqual([
      { rightId: "FIBU_EINGANGSRECHNUNGEN", value: null, booleanValue: null },
    ]);
  });
});
