import { describe, expect, it } from "vitest";
import { descriptionBlocks } from "./description-blocks";

describe("descriptionBlocks", () => {
  it("reflows wrapped lines into paragraphs and list items", () => {
    const text = [
      "Mode of the export:",
      "- not given (default): csv for new installations, xlsx if the",
      "  template already exists.",
      "- xlsx: all sheets are exported as Excel.",
      "The nightly export keeps running",
      "in both modes.",
    ].join("\n");
    expect(descriptionBlocks(text)).toEqual([
      { kind: "paragraph", text: "Mode of the export:" },
      {
        kind: "list",
        items: [
          "not given (default): csv for new installations, xlsx if the template already exists.",
          "xlsx: all sheets are exported as Excel.",
        ],
      },
      {
        kind: "paragraph",
        text: "The nightly export keeps running in both modes.",
      },
    ]);
  });

  it("keeps cron expressions and placeholders as they are, a blank line ends a paragraph", () => {
    expect(
      descriptionBlocks("Cron: 0 0 * * * *\n\nSee <home>/report.txt")
    ).toEqual([
      { kind: "paragraph", text: "Cron: 0 0 * * * *" },
      { kind: "paragraph", text: "See <home>/report.txt" },
    ]);
  });
});
