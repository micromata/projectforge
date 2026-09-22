// @vitest-environment jsdom
// vitest runs in a node environment by default (see vitest.config); this file needs a DOM for DOMParser.
import { describe, expect, it } from "vitest";
import { parseTooltipHtml } from "./tooltip-html";

describe("parseTooltipHtml", () => {
  it("reads label/value pairs and strips the trailing colon of the label", () => {
    const html =
      "<table><tr><th>Kalender:</th><td>Team A</td></tr>" +
      "<tr><th>Ort:</th><td>Kassel</td></tr></table>";
    expect(parseTooltipHtml(html)).toEqual([
      { label: "Kalender", value: "Team A", multiline: false },
      { label: "Ort", value: "Kassel", multiline: false },
    ]);
  });

  it("drops rows whose value is empty", () => {
    const html =
      "<table><tr><th>Kalender:</th><td>Team A</td></tr>" +
      "<tr><th>Ort:</th><td></td></tr></table>";
    expect(parseTooltipHtml(html)).toEqual([
      { label: "Kalender", value: "Team A", multiline: false },
    ]);
  });

  it("turns <br> into newlines and marks the row multiline (participant list)", () => {
    const html =
      "<table><tr><th>Teilnehmer:</th><td>Anna<br>Bob<br>Cara</td></tr></table>";
    const rows = parseTooltipHtml(html);
    expect(rows[0].value).toBe("Anna\nBob\nCara");
    expect(rows[0].multiline).toBe(true);
  });

  it("keeps <pre> content as text and marks it multiline (task path)", () => {
    const html =
      "<table><tr><th>Aufgabe:</th><td><pre>Root\n  Sub\n    Leaf</pre></td></tr></table>";
    const rows = parseTooltipHtml(html);
    expect(rows[0].label).toBe("Aufgabe");
    expect(rows[0].value).toContain("Leaf");
    expect(rows[0].multiline).toBe(true);
  });

  it("reduces an embedded anchor to its text (task path as HTML)", () => {
    const html =
      '<table><tr><th>Aufgabe:</th><td><a href="/wa/taskTree">ACME / Backend</a></td></tr></table>';
    expect(parseTooltipHtml(html)).toEqual([
      { label: "Aufgabe", value: "ACME / Backend", multiline: false },
    ]);
  });
});
