"use client";

import { forwardRef, useEffect, useImperativeHandle, useRef } from "react";
import { EditorContent, useEditor } from "@tiptap/react";
import type { EditorView } from "@tiptap/pm/view";
import { pasteMarkdownAs } from "@/components/shared/rich-text-extensions";
import {
  RICH_TEXT_MUTED_CLASSES,
  sanitizeChatHtml,
} from "@/components/shared/rich-text";
import { cn } from "@/lib/utils";
import { chatEditorExtensions } from "./chat-editor-extensions";
import { ChatFormatToolbar } from "./chat-format-toolbar";
import { MentionSuggestionList } from "./mention-suggestion-list";
import type { ChatEditorHandle } from "./types";
import { useMentionSuggestion } from "./use-mention-suggestion";

const EMPTY_DOCUMENT = "<p></p>";

/** Whether the selection is in a code block (several lines of code). */
function isInCodeBlock(view: EditorView): boolean {
  return view.state.selection.$from.parent.type.name === "codeBlock";
}

/**
 * A paragraph of just ``` (a language after it is dropped, there is no highlighting) turned into a code block, as
 * the input rule of TipTap does on a space after it: a new line (Enter) after ``` starts the block as well.
 */
function fenceToCodeBlock(view: EditorView): boolean {
  const { $from, empty } = view.state.selection;
  const paragraph = $from.parent;
  if (!empty || paragraph.type.name !== "paragraph") return false;
  if (!/^```[\w-]*$/.test(paragraph.textContent)) return false;
  if ($from.parentOffset !== paragraph.content.size) return false;
  const start = $from.start();
  view.dispatch(
    view.state.tr
      .delete(start, $from.end())
      .setBlockType(start, start, view.state.schema.nodes.codeBlock)
  );
  return true;
}

/** The files of a paste, if any. */
function pastedFiles(data: DataTransfer | null): File[] {
  return Array.from(data?.files ?? []);
}

/**
 * The editor of a chat message (TipTap) with its formatting toolbar: bold, italic, underline, strike-through,
 * colours, lists, links, code and mentions with suggestions (`@`). Enter submits, Shift+Enter starts a new line
 * (paragraph, list item); after ``` Enter starts a code block, in which Enter starts a new line as well and
 * Ctrl/Cmd+Enter submits (which it does anywhere). Pasted files go
 * to [onFiles]. Dropped files are left to the drop zone around it (ChatComposer), the editor only keeps them
 * from being inserted. The content is handed out
 * via the ref ([ChatEditorHandle]) when submitting, not on every key stroke; [onChange] only reports whether
 * there is any.
 */
export const ChatEditor = forwardRef<
  ChatEditorHandle,
  {
    conversationId: number;
    initialHtml?: string;
    placeholder: string;
    ariaLabel: string;
    onSubmit: () => void;
    onFiles?: (files: File[]) => void;
    onChange?: (html: string) => void;
    disabled?: boolean;
    autoFocus?: boolean;
    className?: string;
  }
>(function ChatEditor(props, ref) {
  const mention = useMentionSuggestion(props.conversationId);
  // The editor keeps the callbacks of its creation; this ref hands it the current ones.
  const propsRef = useRef(props);
  useEffect(() => {
    propsRef.current = props;
  });
  const editor = useEditor({
    immediatelyRender: false,
    extensions: chatEditorExtensions(
      props.placeholder,
      mention.items,
      mention.render
    ),
    content: sanitizeChatHtml(props.initialHtml ?? ""),
    editable: !props.disabled,
    autofocus: props.autoFocus ? "end" : false,
    onUpdate: ({ editor }) => {
      const html = editor.getHTML();
      propsRef.current.onChange?.(html === EMPTY_DOCUMENT ? "" : html);
    },
    editorProps: {
      attributes: {
        "aria-label": props.ariaLabel,
        class: cn(
          "rich-text-editor max-h-48 min-h-9 overflow-y-auto px-3 py-2 text-sm font-normal focus:outline-none [&_.chat-mention]:font-semibold [&_.chat-mention]:text-primary",
          RICH_TEXT_MUTED_CLASSES
        ),
      },
      handleKeyDown: (view, event) => {
        if (event.key !== "Enter" || event.isComposing) return false;
        const send = event.ctrlKey || event.metaKey;
        if (mention.isOpen()) return false; // Enter chooses the suggestion.
        if (!send && fenceToCodeBlock(view)) {
          event.preventDefault();
          return true;
        }
        // A new line (ChatNewLine), in a code block by Enter as well.
        if (!send && (event.shiftKey || isInCodeBlock(view))) return false;
        event.preventDefault();
        propsRef.current.onSubmit();
        return true;
      },
      handlePaste: (view, event) => {
        const files = pastedFiles(event.clipboardData);
        if (files.length > 0 && propsRef.current.onFiles) {
          propsRef.current.onFiles(files);
          return true;
        }
        // A text pasted into a code block is code, whatever it looks like.
        return (
          !isInCodeBlock(view) && pasteMarkdownAs(view, event, sanitizeChatHtml)
        );
      },
      // Handled by the drop zone around the editor, which the drop still reaches.
      handleDrop: (_view, event) => (event.dataTransfer?.files.length ?? 0) > 0,
    },
  });
  useEffect(() => {
    editor?.setEditable(!props.disabled, false);
  }, [props.disabled, editor]);

  useImperativeHandle(
    ref,
    () => ({
      clear: () => editor?.commands.clearContent(true),
      focus: () => editor?.commands.focus("end"),
      isEmpty: () => !editor || editor.isEmpty,
      getHtml: () => {
        const html = editor?.getHTML() ?? "";
        return html === EMPTY_DOCUMENT ? "" : html;
      },
    }),
    [editor]
  );

  return (
    <div className={cn("min-w-0 flex-1", props.className)}>
      <ChatFormatToolbar editor={editor} />
      <EditorContent editor={editor} />
      <MentionSuggestionList
        state={mention.state}
        onChoose={mention.choose}
        onClose={mention.close}
      />
    </div>
  );
});
