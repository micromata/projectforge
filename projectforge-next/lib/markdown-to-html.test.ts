import { describe, expect, it } from "vitest";
import { looksLikeMarkdown, markdownToHtml } from "./markdown-to-html";

describe("markdownToHtml", () => {
  it("converts headings, emphasis and lists", () => {
    expect(markdownToHtml("## Title\n\n**bold** text\n\n- a\n- b")).toBe(
      "<h2>Title</h2>\n<p><strong>bold</strong> text</p>\n<ul>\n<li>a</li>\n<li>b</li>\n</ul>"
    );
  });

  it("keeps a single line break and inline html", () => {
    expect(
      markdownToHtml('first\n<span style="color: red">31.01.2026</span>')
    ).toBe('<p>first<br><span style="color: red">31.01.2026</span></p>');
  });

  it("converts a table", () => {
    const html = markdownToHtml("| a | b |\n| - | - |\n| 1 | 2 |");
    expect(html).toContain("<th>a</th>");
    expect(html).toContain("<td>2</td>");
  });

  it("wraps plain text into a paragraph", () => {
    expect(markdownToHtml("just text")).toBe("<p>just text</p>");
  });
});

describe("looksLikeMarkdown", () => {
  it.each([
    "# Heading",
    "text\n## Heading",
    "- item",
    "1. item",
    "some **bold** word",
    "a [link](https://example.org)",
    "| a | b |",
  ])("recognizes %j", (text) => expect(looksLikeMarkdown(text)).toBe(true));

  it.each(["plain text", "a * b * c", "3.5 hours", "#hashtag"])(
    "leaves %j as plain text",
    (text) => expect(looksLikeMarkdown(text)).toBe(false)
  );
});
