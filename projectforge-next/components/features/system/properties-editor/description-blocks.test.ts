import { describe, expect, it } from "vitest";
import { descriptionBlocks } from "./description-blocks";

describe("descriptionBlocks", () => {
  it("reflows wrapped lines into paragraphs and list items", () => {
    const text = [
      "Store of the files:",
      "- not given (default): db for new installations, jcr if the JCR",
      "  already contains data.",
      "- jcr: all files are stored in the JCR.",
      "The nightly JCR backup keeps running",
      "in both modes.",
    ].join("\n");
    expect(descriptionBlocks(text)).toEqual([
      { kind: "paragraph", text: "Store of the files:" },
      {
        kind: "list",
        items: [
          "not given (default): db for new installations, jcr if the JCR already contains data.",
          "jcr: all files are stored in the JCR.",
        ],
      },
      {
        kind: "paragraph",
        text: "The nightly JCR backup keeps running in both modes.",
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
