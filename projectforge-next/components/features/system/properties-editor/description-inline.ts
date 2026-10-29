/** A piece of a description line: plain text, `code`, **bold** or a link. */
export type InlineSegment =
  | { kind: "text"; text: string }
  | { kind: "code"; text: string }
  | { kind: "bold"; text: string }
  | { kind: "link"; text: string; href: string };

// Order matters: a markdown link before the bare URL it contains, code before everything else (its
// content stays literal).
const TOKEN =
  /`([^`]+)`|\*\*([^*]+)\*\*|\[([^\]]+)\]\((https?:\/\/[^\s)]+)\)|(https?:\/\/[^\s<>`]+)/g;

/** Trailing sentence punctuation isn't part of a bare URL. */
const TRAILING_PUNCTUATION = /[.,;:!?)]+$/;

/**
 * Splits a description line into text, `code`, **bold**, `[text](url)` and bare http(s) URLs.
 *
 * Deliberately not full markdown: the descriptions are full of `*` (cron expressions), `_` and `<home>`,
 * which stay literal. Only http(s) links are recognized (no `javascript:`), so the result can be rendered
 * as React elements without any html.
 */
export function inlineSegments(line: string): InlineSegment[] {
  const segments: InlineSegment[] = [];
  let pos = 0;
  const pushText = (text: string) => {
    if (text) segments.push({ kind: "text", text });
  };
  for (const match of line.matchAll(TOKEN)) {
    const [whole, code, bold, linkText, linkHref, bare] = match;
    pushText(line.substring(pos, match.index));
    pos = match.index + whole.length;
    if (code) {
      segments.push({ kind: "code", text: code });
    } else if (bold) {
      segments.push({ kind: "bold", text: bold });
    } else if (linkText) {
      segments.push({ kind: "link", text: linkText, href: linkHref });
    } else {
      const trailing = TRAILING_PUNCTUATION.exec(bare)?.[0] ?? "";
      const url = bare.substring(0, bare.length - trailing.length);
      segments.push({ kind: "link", text: url, href: url });
      pos -= trailing.length;
    }
  }
  pushText(line.substring(pos));
  return segments;
}
