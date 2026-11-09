// @vitest-environment jsdom
import { describe, expect, it } from "vitest";
import { findUrls, linkifyHtml } from "./linkify-html";

describe("findUrls", () => {
  it("finds urls with a scheme or www., not bare names", () => {
    expect(
      findUrls(
        "see https://x.org/a(b). and www.projectforge.org, pfDev.sh or example.com"
      )
    ).toEqual([
      { start: 4, end: 22, href: "https://x.org/a(b)" },
      { start: 28, end: 48, href: "https://www.projectforge.org" },
    ]);
  });
});

describe("linkifyHtml", () => {
  it("links the urls of the text, opened in a new tab", () => {
    expect(linkifyHtml("<p>Docs: https://projectforge.org/docs.</p>")).toBe(
      '<p>Docs: <a href="https://projectforge.org/docs" target="_blank" rel="noopener noreferrer">https://projectforge.org/docs</a>.</p>'
    );
  });

  it("leaves links, code and mentions alone", () => {
    const html =
      '<p><a href="https://a.org">https://a.org</a> <code>https://b.org</code> <span data-type="mention" data-id="1">www.c.org</span></p><pre><code>https://d.org</code></pre>';
    expect(linkifyHtml(html)).toBe(html);
  });

  it("keeps a text without urls as it is", () => {
    expect(linkifyHtml("<p>No <strong>links</strong> here.</p>")).toBe(
      "<p>No <strong>links</strong> here.</p>"
    );
  });
});
