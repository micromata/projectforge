import { describe, expect, it } from "vitest";
import { buildTableText, tsvField } from "./table-clipboard";

describe("table clipboard", () => {
  it("leaves plain values unquoted", () => {
    expect(tsvField("12,50 €")).toBe("12,50 €");
  });
  it("quotes tabs, line breaks and quotes", () => {
    expect(tsvField('a"b')).toBe('"a""b"');
    expect(tsvField("a\tb")).toBe('"a\tb"');
    expect(tsvField("a\nb")).toBe('"a\nb"');
  });
  it("builds header row, rows and an escaped html table", () => {
    const { tsv, html } = buildTableText(["Name", "Sum"], [["A<b>", ""]]);
    expect(tsv).toBe("Name\tSum\r\nA<b>\t");
    expect(html).toContain("<th>Name</th><th>Sum</th>");
    expect(html).toContain("<td>A&lt;b&gt;</td><td></td>");
  });
});
