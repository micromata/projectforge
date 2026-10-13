import type { EditorView } from "@tiptap/pm/view";
import StarterKit from "@tiptap/starter-kit";
import { TableKit } from "@tiptap/extension-table";
import { Color, TextStyle } from "@tiptap/extension-text-style";
import { Placeholder } from "@tiptap/extensions";
import { sanitizeRichText } from "@/components/shared/rich-text";
import { looksLikeMarkdown, markdownToHtml } from "@/lib/markdown-to-html";

/** The heading levels offered: enough to structure a note, more would only be noise. */
export const RICH_TEXT_HEADING_LEVELS = [1, 2, 3, 4] as const;

/** The TipTap extensions of [RichTextEditor], i.e. the markup a rich text may contain. */
export function richTextExtensions(placeholder: string) {
  return [
    StarterKit.configure({
      heading: { levels: [...RICH_TEXT_HEADING_LEVELS] },
      code: false,
      codeBlock: false,
      blockquote: false,
      horizontalRule: false,
      link: { openOnClick: false, autolink: true },
    }),
    // No column resizing: a stored width wouldn't survive the sanitizing anyway (only colours do).
    TableKit.configure({ table: { resizable: false } }),
    TextStyle,
    Color,
    Placeholder.configure({ placeholder }),
  ];
}

/**
 * Pastes a plain text with markdown markup (e.g. copied from a chat or a README) formatted instead of
 * with its `**` and `##`. A paste carrying HTML (from a web page or an office document) is left to the
 * editor, as is a plain text without markup.
 */
export function pasteMarkdown(view: EditorView, event: ClipboardEvent) {
  const data = event.clipboardData;
  if (!data || data.getData("text/html")) return false;
  const text = data.getData("text/plain");
  if (!looksLikeMarkdown(text)) return false;
  // Without the event: pasteHTML hands it to this handler again, which would convert it endlessly.
  return view.pasteHTML(sanitizeRichText(markdownToHtml(text)));
}
