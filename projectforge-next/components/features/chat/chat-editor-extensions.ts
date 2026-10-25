import Mention from "@tiptap/extension-mention";
import type { SuggestionOptions } from "@tiptap/suggestion";
import StarterKit from "@tiptap/starter-kit";
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

export type MentionRender = ReturnType<
  NonNullable<SuggestionOptions<ChatRef>["render"]>
>;

/**
 * The markup of a chat message: paragraphs, bold, italic, strike-through, lists, links and mentions. No
 * headings, tables or code: it is a chat, not a document (see sanitizeChatHtml).
 */
export function chatEditorExtensions(
  placeholder: string,
  mentionItems: (query: string) => Promise<ChatRef[]>,
  mentionRender: () => MentionRender
) {
  return [
    StarterKit.configure({
      heading: false,
      code: false,
      codeBlock: false,
      blockquote: false,
      horizontalRule: false,
      link: { openOnClick: false, autolink: true },
    }),
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
