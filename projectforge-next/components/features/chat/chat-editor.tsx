"use client";

import { forwardRef, useEffect, useImperativeHandle, useRef } from "react";
import { EditorContent, useEditor } from "@tiptap/react";
import { pasteMarkdown } from "@/components/shared/rich-text-extensions";
import {
  RICH_TEXT_MUTED_CLASSES,
  sanitizeChatHtml,
} from "@/components/shared/rich-text";
import { cn } from "@/lib/utils";
import { chatEditorExtensions } from "./chat-editor-extensions";
import { MentionSuggestionList } from "./mention-suggestion-list";
import type { ChatEditorHandle } from "./types";
import { useMentionSuggestion } from "./use-mention-suggestion";

const EMPTY_DOCUMENT = "<p></p>";

/** The images of a paste or a drop, if any. */
function imageFiles(data: DataTransfer | null): File[] {
  return Array.from(data?.files ?? []).filter((file) =>
    file.type.startsWith("image/")
  );
}

/**
 * The editor of a chat message (TipTap): bold, italic, lists, links and mentions with suggestions (`@`). Enter
 * submits, Shift+Enter starts a new line; pasted or dropped images go to [onImages]. The content is handed out
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
    onImages?: (files: File[]) => void;
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
      handleKeyDown: (_view, event) => {
        if (event.key !== "Enter" || event.shiftKey || event.isComposing)
          return false;
        if (mention.isOpen()) return false; // Enter chooses the suggestion.
        event.preventDefault();
        propsRef.current.onSubmit();
        return true;
      },
      handlePaste: (view, event) => {
        const files = imageFiles(event.clipboardData);
        if (files.length > 0 && propsRef.current.onImages) {
          propsRef.current.onImages(files);
          return true;
        }
        return pasteMarkdown(view, event);
      },
      handleDrop: (_view, event) => {
        const files = imageFiles(event.dataTransfer);
        if (files.length === 0 || !propsRef.current.onImages) return false;
        event.preventDefault();
        propsRef.current.onImages(files);
        return true;
      },
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
      <EditorContent editor={editor} />
      <MentionSuggestionList
        state={mention.state}
        onChoose={mention.choose}
        onClose={mention.close}
      />
    </div>
  );
});
