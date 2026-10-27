import { Extension } from "@tiptap/react";
import { Plugin } from "@tiptap/pm/state";
import { Decoration, DecorationSet } from "@tiptap/pm/view";
import {
  CUSTOM_EMOJI_PATTERN,
  customEmojiClass,
  isCustomEmoji,
} from "@/components/shared/emoji/custom-emoji";

/**
 * Shows the shortcodes of the custom emoji (`:siren-blue:`) as the emoji already while writing. The document keeps
 * the text, so the message is stored and sanitized as before; only the display is replaced (decoration, see
 * `.chat-emoji-code` in globals.css). Not in code.
 */
export const EmojiShortcodes = Extension.create({
  name: "emojiShortcodes",
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
