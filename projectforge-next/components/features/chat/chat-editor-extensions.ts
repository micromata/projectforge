import { Extension } from "@tiptap/react";
import Mention from "@tiptap/extension-mention";
import type { SuggestionOptions } from "@tiptap/suggestion";
import StarterKit from "@tiptap/starter-kit";
import { Color, TextStyle } from "@tiptap/extension-text-style";
import { Placeholder } from "@tiptap/extensions";
import type { ChatRef } from "@/lib/rs/chat-types";

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
