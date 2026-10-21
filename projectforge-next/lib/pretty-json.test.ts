import { describe, expect, it } from "vitest";
import { isValidJson, prettyJson } from "./pretty-json";

describe("prettyJson", () => {
  it("indents valid JSON", () => {
    expect(prettyJson('{"a":1,"b":[true]}')).toBe(
      '{\n  "a": 1,\n  "b": [\n    true\n  ]\n}'
    );
  });

  it("keeps invalid or blank text as it is", () => {
    expect(prettyJson('{"a":')).toBe('{"a":');
    expect(prettyJson("  ")).toBe("  ");
    expect(prettyJson(null)).toBe("");
    expect(prettyJson(undefined)).toBe("");
  });
});

describe("isValidJson", () => {
  it("accepts JSON and blank text only", () => {
    expect(isValidJson('{"a":1}')).toBe(true);
    expect(isValidJson("")).toBe(true);
    expect(isValidJson(null)).toBe(true);
    expect(isValidJson("{broken")).toBe(false);
  });
});
