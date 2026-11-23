/**
 * The clipboard form of a table: tab separated text and an html table. Spreadsheets (Excel, Numbers,
 * LibreOffice) take the html as a table with a header row, a plain text target (a terminal, an editor)
 * gets the TSV, which every spreadsheet splits into cells as well.
 */
export interface TableText {
  tsv: string;
  html: string;
}

/**
 * One TSV field. Quoted (with doubled quotes) only where it has to be: a tab, a line break or a quote
 * would otherwise break the cell grid when pasted. Newlines inside a cell become a space-free quoted
 * line break, which Excel and Numbers keep inside the cell.
 */
export function tsvField(value: string): string {
  return /[\t\r\n"]/.test(value) ? `"${value.replace(/"/g, '""')}"` : value;
}

export function escapeHtml(value: string): string {
  return value
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;");
}

export function buildTableText(headers: string[], rows: string[][]): TableText {
  const tsv = [headers, ...rows]
    .map((line) => line.map(tsvField).join("\t"))
    .join("\r\n");
  const cell = (tag: string, value: string) =>
    `<${tag}>${escapeHtml(value).replace(/\r?\n/g, "<br>")}</${tag}>`;
  const html =
    "<table><thead><tr>" +
    headers.map((h) => cell("th", h)).join("") +
    "</tr></thead><tbody>" +
    rows
      .map((r) => "<tr>" + r.map((c) => cell("td", c)).join("") + "</tr>")
      .join("") +
    "</tbody></table>";
  return { tsv, html };
}

/** Puts both flavors on the clipboard; plain text only where the browser has no ClipboardItem. */
export async function writeTableToClipboard({
  tsv,
  html,
}: TableText): Promise<void> {
  if (typeof ClipboardItem === "undefined") {
    await navigator.clipboard.writeText(tsv);
    return;
  }
  await navigator.clipboard.write([
    new ClipboardItem({
      "text/plain": new Blob([tsv], { type: "text/plain" }),
      "text/html": new Blob([html], { type: "text/html" }),
    }),
  ]);
}
