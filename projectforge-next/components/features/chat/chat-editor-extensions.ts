import { Extension } from "@tiptap/react";
import Mention from "@tiptap/extension-mention";
import type { SuggestionOptions } from "@tiptap/suggestion";
import StarterKit from "@tiptap/starter-kit";
import { Color, TextStyle } from "@tiptap/extension-text-style";
import { Placeholder } from "@tiptap/extensions";
import { Plugin } from "@tiptap/pm/state";
import { Decoration, DecorationSet } from "@tiptap/pm/view";
import type { ChatRef } from "@/lib/rs/chat-types";
import {
  CUSTOM_EMOJI_PATTERN,
  customEmojiClass,
  isCustomEmoji,
} from "./custom-emoji";

/**
 * A mention (`<span data-type="mention" data-id="…">@Name</span>`). The backend keeps only `data-type` and
 * `data-id` (ChatHtmlSanitizer) and writes the name as text, so the label of a stored mention is its text.
 */
const ChatMention = Mention.extend({
  addAttributes() {
    const parent = this.parent?.() ?? {};
    return {
      ...parent,
      label: {
        default: null,
        parseHTML: (element: HTMLElement) =>
          element.getAttribute("data-label") ??
          element.textContent?.replace(/^@/, "") ??
          null,
        renderHTML: () => ({}),
      },
    };
  },
});

/**
 * Shift+Enter does what Enter does in a document, Enter submitting the message (ChatEditor): a new line in a code
 * block, a new list item (an empty one ends the list), else a new paragraph. Not a line break within the paragraph:
 * a code block from the toolbar or from ``` then takes just the new line, not the whole text before it.
 */
const ChatNewLine = Extension.create({
  name: "chatNewLine",
  priority: 1000, // Before the line break of HardBreak.
  addKeyboardShortcuts() {
    return {
      "Shift-Enter": () =>
        this.editor.commands.first(({ commands }) => [
          () => commands.newlineInCode(),
          () => commands.splitListItem("listItem"),
          () => commands.createParagraphNear(),
          () => commands.liftEmptyBlock(),
          () => commands.splitBlock(),
        ]),
    };
  },
});

/**
 * Shows the shortcodes of the custom emoji (`:siren-blue:`) as the emoji already while writing. The document keeps
 * the text, so the message is stored and sanitized as before; only the display is replaced (decoration, see
 * `.chat-emoji-code` in globals.css). Not in code.
 */
const ChatEmojiShortcodes = Extension.create({
  name: "chatEmojiShortcodes",
  addKeyboardShortcuts() {
    // Backspace and Delete take the whole shortcode, the emoji behaves like one character.
    const remove = (forward: boolean) => () => {
      const { selection } = this.editor.state;
      if (!selection.empty) return false;
      const $pos = selection.$from;
      // One character per leaf (mention, line break), so the offsets are the positions in the document.
      const text = $pos.parent.textBetween(
        0,
        $pos.parent.content.size,
        undefined,
        "\ufffc"
      );
      const offset = $pos.parentOffset;
      for (const match of text.matchAll(CUSTOM_EMOJI_PATTERN)) {
        const start = match.index ?? 0;
        const end = start + match[0].length;
        if (
          !isCustomEmoji(match[0]) ||
          (forward ? offset !== start : offset !== end)
        )
          continue;
        const from = $pos.start() + start;
        return this.editor.commands.deleteRange({
          from,
          to: from + match[0].length,
        });
      }
      return false;
    };
    return { Backspace: remove(false), Delete: remove(true) };
  },
  addProseMirrorPlugins() {
    return [
      new Plugin({
        props: {
          decorations: ({ doc }) => {
            const decorations: Decoration[] = [];
            doc.descendants((node, pos, parent) => {
              if (!node.isText || !node.text || parent?.type.spec.code) return;
              if (node.marks.some((mark) => mark.type.name === "code")) return;
              for (const match of node.text.matchAll(CUSTOM_EMOJI_PATTERN)) {
                if (!isCustomEmoji(match[0])) continue;
                const from = pos + (match.index ?? 0);
                decorations.push(
                  Decoration.inline(from, from + match[0].length, {
                    class: `${customEmojiClass(match[0])} chat-emoji-code`,
                  })
                );
              }
            });
            return DecorationSet.create(doc, decorations);
          },
        },
      }),
    ];
  },
});

export type MentionRender = ReturnType<
  NonNullable<SuggestionOptions<ChatRef>["render"]>
>;

/**
 * The markup of a chat message: paragraphs, bold, italic, strike-through, text colours, lists, links, code (inline
 * and as a block of several lines, without highlighting) and mentions. No headings or tables: it is a chat, not a
 * document (see sanitizeChatHtml).
 */
export function chatEditorExtensions(
  placeholder: string,
  mentionItems: (query: string) => Promise<ChatRef[]>,
  mentionRender: () => MentionRender
) {
  return [
    StarterKit.configure({
      heading: false,
      blockquote: false,
      horizontalRule: false,
      // Tab indents (by spaces) in a code block, else it leaves the editor.
      codeBlock: { enableTabIndentation: true },
      link: { openOnClick: false, autolink: true },
    }),
    ChatNewLine,
    ChatEmojiShortcodes,
    TextStyle,
    Color,
    Placeholder.configure({ placeholder }),
    ChatMention.configure({
      HTMLAttributes: { class: "chat-mention" },
      suggestion: {
        char: "@",
        items: ({ query }) => mentionItems(query),
        render: mentionRender,
      },
    }),
  ];
}
