/** A paragraph or a bullet list of a parameter description. */
export type DescriptionBlock =
  | { kind: "paragraph"; text: string }
  | { kind: "list"; items: string[] };

/**
 * Splits a description from application.properties into paragraphs and bullet lists, reflowing the
 * lines its comment was wrapped into. The comments follow one convention (see the header of
 * application.properties): `- ` starts a list item, an indented line continues the line above, and any
 * other line continues a paragraph. A blank line ends a paragraph.
 *
 * No full markdown on purpose: the descriptions are full of `*` (cron expressions), `_` and `<home>`,
 * which markdown would turn into emphasis or swallow as HTML. The inline subset (`code`, **bold**,
 * links) is handled by `inlineSegments`.
 */
export function descriptionBlocks(description: string): DescriptionBlock[] {
  const blocks: DescriptionBlock[] = [];
  let paragraphEnded = true;
  for (const rawLine of description.split("\n")) {
    const line = rawLine.trim();
    const last = blocks[blocks.length - 1];
    if (line === "") {
      paragraphEnded = true;
    } else if (line.startsWith("- ")) {
      const item = line.substring(2).trim();
      if (last?.kind === "list") last.items.push(item);
      else blocks.push({ kind: "list", items: [item] });
      paragraphEnded = false;
    } else if (/^\s/.test(rawLine) && last?.kind === "list") {
      last.items[last.items.length - 1] += ` ${line}`;
    } else if (!paragraphEnded && last?.kind === "paragraph") {
      last.text += ` ${line}`;
    } else {
      blocks.push({ kind: "paragraph", text: line });
      paragraphEnded = false;
    }
  }
  return blocks;
}
