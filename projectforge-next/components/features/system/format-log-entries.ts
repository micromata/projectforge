/**
 * Pretty-prints a one-line log snippet (a toString of nested objects/collections) for readability,
 * ported from Wicket's `AdminPage.formatLogEntries`: a line break plus indentation after every comma,
 * a deeper indent inside brackets. Pure client-side — the classic page had no backend for it either.
 *
 * The Wicket version wrote HTML (`<br/>`, `&nbsp;`); here it produces plain text with real newlines
 * and spaces so it can be shown in a `<pre>` without rendering untrusted markup.
 */
export function formatLogEntries(input: string): string {
  let indent = 0;
  let out = "";
  for (const char of input) {
    out += char;
    if (char === ",") {
      out += "\n" + INDENT.repeat(indent);
    } else if (char === "[") {
      indent++;
      out += "\n" + INDENT.repeat(indent);
    } else if (char === "]") {
      indent = Math.max(0, indent - 1);
      out += "\n";
    }
  }
  return out;
}

/** One indentation level — the four spaces the Wicket page rendered as four `&nbsp;`. */
const INDENT = "    ";
