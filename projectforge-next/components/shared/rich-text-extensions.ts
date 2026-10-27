import type { Extensions } from "@tiptap/react";
import type { EditorView } from "@tiptap/pm/view";
import StarterKit from "@tiptap/starter-kit";
import { TableKit } from "@tiptap/extension-table";
import { Color, TextStyle } from "@tiptap/extension-text-style";
import { Placeholder } from "@tiptap/extensions";
import { EmojiShortcodes } from "@/components/shared/emoji/emoji-shortcodes";
import { sanitizeRichText } from "@/components/shared/rich-text";
import { looksLikeMarkdown, markdownToHtml } from "@/lib/markdown-to-html";

/** The heading levels offered: enough to structure a note, more would only be noise. */
export const RICH_TEXT_HEADING_LEVELS = [1, 2, 3, 4] as const;

/**
 * What a rich text editor offers, i.e. the markup it may produce (the sanitizers must keep it, see
 * [sanitizeRichText] and [sanitizeChatHtml]). The same editor serves a note, a room description and a chat message.
 */
export interface RichTextFeatures {
  /** Headings (levels 1 to 4). */
  headings?: boolean;
  tables?: boolean;
  /** Strike-through, code (inline) and code blocks (several lines, without highlighting). */
  code?: boolean;
  /** The emoji picker, the custom emoji (`:siren-blue:`) shown as the emoji while writing. */
  emoji?: boolean;
}

/** A note, a mail body, a tooltip hint: headings and tables. */
export const NOTE_FEATURES: RichTextFeatures = { headings: true, tables: true };

/** A room description: a note that may contain code and emoji as well. */
export const DESCRIPTION_FEATURES: RichTextFeatures = {
  headings: true,
  tables: true,
  code: true,
  emoji: true,
};

/** A chat message: no headings, it is a chat, not a document. */
export const CHAT_MESSAGE_FEATURES: RichTextFeatures = {
  tables: true,
  code: true,
  emoji: true,
};

/**
 * The TipTap extensions of [RichTextEditor] and of the chat editor, i.e. the markup a rich text may contain.
 * [extra] are added by the caller (mentions in a chat message).
 */
export function richTextExtensions(
  placeholder: string,
  features: RichTextFeatures = NOTE_FEATURES,
  extra: Extensions = []
): Extensions {
  return [
    StarterKit.configure({
      heading: features.headings
        ? { levels: [...RICH_TEXT_HEADING_LEVELS] }
        : false,
      code: features.code ? undefined : false,
      // Tab indents (by spaces) in a code block, else it leaves the editor.
      codeBlock: features.code ? { enableTabIndentation: true } : false,
      blockquote: false,
      horizontalRule: false,
      link: { openOnClick: false, autolink: true },
    }),
    // No column resizing: a stored width wouldn't survive the sanitizing anyway (only colours do).
    ...(features.tables
      ? [TableKit.configure({ table: { resizable: false } })]
      : []),
    TextStyle,
    Color,
    Placeholder.configure({ placeholder }),
    ...(features.emoji ? [EmojiShortcodes] : []),
    ...extra,
  ];
}

/**
 * Pastes a plain text with markdown markup (e.g. copied from a chat or a README) formatted instead of
 * with its `**` and `##`. A paste carrying HTML (from a web page or an office document) is left to the
 * editor, as is a plain text without markup.
 */
export function pasteMarkdown(view: EditorView, event: ClipboardEvent) {
  return pasteMarkdownAs(view, event, sanitizeRichText);
}

/** [pasteMarkdown] for an editor of other markup (the chat), sanitized by [sanitize]. */
export function pasteMarkdownAs(
  view: EditorView,
  event: ClipboardEvent,
  sanitize: (html: string) => string
) {
  const data = event.clipboardData;
  if (!data || data.getData("text/html")) return false;
  const text = data.getData("text/plain");
  if (!looksLikeMarkdown(text)) return false;
  // Without the event: pasteHTML hands it to this handler again, which would convert it endlessly.
  return view.pasteHTML(sanitize(markdownToHtml(text)));
}
