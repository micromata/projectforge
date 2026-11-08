// @vitest-environment jsdom
import { describe, expect, it } from "vitest";
import { Editor } from "@tiptap/core";
import { richTextExtensions } from "./rich-text-extensions";

function editor(html: string) {
  return new Editor({
    extensions: richTextExtensions("", { code: true }),
    content: html,
  });
}

describe("LinkTextSync", () => {
  it("follows an edited url", () => {
    const e = editor(
      '<p><a href="https://www.projectforge.org">www.projectforge.org</a> Test</p>'
    );
    // Replaces the "org" of ".org" by "de" within the link (positions start at 1 in the paragraph).
    const start = e.getText().indexOf(".org") + 2;
    e.chain()
      .setTextSelection({ from: start, to: start + 3 })
      .insertContent("de")
      .run();
    expect(e.getHTML()).toContain('href="https://www.projectforge.de"');
    expect(e.getHTML()).toContain(">www.projectforge.de</a>");
    e.destroy();
  });

  it("keeps the target of a link whose text isn't a url", () => {
    const e = editor('<p><a href="https://example.com/a">here</a></p>');
    e.chain().setTextSelection(3).insertContent("x").run();
    expect(e.getHTML()).toContain('href="https://example.com/a"');
    e.destroy();
  });
});
