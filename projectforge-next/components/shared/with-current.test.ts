import { describe, expect, it } from "vitest";
import { withCurrent } from "./with-current";

const alice = { id: 1, displayName: "Alice Active" };
const otto = { id: 2, displayName: "Otto Gone", deactivated: true };

describe("withCurrent", () => {
  it("leaves the entries alone without a current value", () => {
    expect(withCurrent([alice], null, "")).toEqual([alice]);
  });

  it("leaves the entries alone when they hold the current value", () => {
    expect(withCurrent([alice, otto], otto, "")).toEqual([alice, otto]);
  });

  it("puts a missing current value on top for the empty term", () => {
    expect(withCurrent([alice], otto, "  ")).toEqual([otto, alice]);
  });

  it("puts it on top for a term that is part of its name", () => {
    expect(withCurrent([], otto, "gON")).toEqual([otto]);
  });

  it("leaves it out for a term it doesn't match", () => {
    expect(withCurrent([alice], otto, "ali")).toEqual([alice]);
  });
});
