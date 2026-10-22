import { describe, expect, it } from "vitest";
import {
  carriesRefs,
  readRefs,
  refTypeOfUrl,
  writeRefs,
} from "./entity-ref-transfer";

/** The part of a DataTransfer the transfer uses — there is none outside a browser. */
function fakeDataTransfer(): DataTransfer {
  const data = new Map<string, string>();
  return {
    get types() {
      return [...data.keys()];
    },
    setData: (format: string, value: string) => void data.set(format, value),
    getData: (format: string) => data.get(format) ?? "",
  } as unknown as DataTransfer;
}

describe("refTypeOfUrl", () => {
  it("takes the first segment of the lookup url", () => {
    expect(refTypeOfUrl("user/autosearch?search=:search")).toBe("user");
    expect(refTypeOfUrl("addressBook/autosearch")).toBe("addressBook");
  });
});

describe("writeRefs / readRefs", () => {
  it("carries the references to a picker of the same kind only", () => {
    const data = fakeDataTransfer();
    writeRefs(data, "addressBook", [
      { id: 1, displayName: "A" },
      { id: 2, displayName: "B", deactivated: true },
    ]);
    expect(carriesRefs(data, "addressBook")).toBe(true);
    expect(carriesRefs(data, "user")).toBe(false);
    expect(readRefs(data, "addressBook")).toEqual([
      { id: 1, displayName: "A" },
      { id: 2, displayName: "B", deactivated: true },
    ]);
    expect(readRefs(data, "user")).toEqual([]);
    expect(data.getData("text/plain")).toBe("A\nB");
  });

  it("leaves out placeholders", () => {
    const data = fakeDataTransfer();
    writeRefs(data, "cost2", [
      { id: -1, displayName: "1.234.56.78" },
      { id: 7, displayName: "1.234.56.79" },
    ]);
    expect(readRefs(data, "cost2")).toEqual([
      { id: 7, displayName: "1.234.56.79" },
    ]);
  });

  it("drops whatever isn't a list of references", () => {
    const data = fakeDataTransfer();
    const mime = "application/x-projectforge-refs+user";
    data.setData(mime, "not json");
    expect(readRefs(data, "user")).toEqual([]);
    data.setData(mime, JSON.stringify({ id: 1, displayName: "A" }));
    expect(readRefs(data, "user")).toEqual([]);
    data.setData(
      mime,
      JSON.stringify([
        { id: "1", displayName: "A" },
        { id: 2 },
        { id: 1.5, displayName: "C" },
        null,
        { id: 3, displayName: "D", deleted: "yes" },
      ])
    );
    expect(readRefs(data, "user")).toEqual([{ id: 3, displayName: "D" }]);
  });
});
