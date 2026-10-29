import { describe, expect, it } from "vitest";
import { inlineSegments } from "./description-inline";

describe("inlineSegments", () => {
  it("renders markdown links, code and bold", () => {
    expect(
      inlineSegments(
        "See [the guide](https://projectforge.org/docs/adminguide/#ldap), `a.b` and **note**"
      )
    ).toEqual([
      { kind: "text", text: "See " },
      {
        kind: "link",
        text: "the guide",
        href: "https://projectforge.org/docs/adminguide/#ldap",
      },
      { kind: "text", text: ", " },
      { kind: "code", text: "a.b" },
      { kind: "text", text: " and " },
      { kind: "bold", text: "note" },
    ]);
  });

  it("links a bare url without its trailing punctuation", () => {
    expect(inlineSegments("Docs: https://projectforge.org/docs.")).toEqual([
      { kind: "text", text: "Docs: " },
      {
        kind: "link",
        text: "https://projectforge.org/docs",
        href: "https://projectforge.org/docs",
      },
      { kind: "text", text: "." },
    ]);
  });

  it("keeps * and _ inside code literal", () => {
    expect(inlineSegments("`5.___.99.*` or `6.9*`")).toEqual([
      { kind: "code", text: "5.___.99.*" },
      { kind: "text", text: " or " },
      { kind: "code", text: "6.9*" },
    ]);
  });

  it("leaves cron expressions and placeholders literal", () => {
    expect(inlineSegments("0 0 * * * * in <home>/a_b")).toEqual([
      { kind: "text", text: "0 0 * * * * in <home>/a_b" },
    ]);
  });

  it("does not link other schemes", () => {
    expect(inlineSegments("[x](javascript:alert(1))")).toEqual([
      { kind: "text", text: "[x](javascript:alert(1))" },
    ]);
  });
});
