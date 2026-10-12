"use client";

import { useEffect, useRef } from "react";
import { EditorContent, useEditor } from "@tiptap/react";
import {
  pasteMarkdown,
  richTextExtensions,
} from "@/components/shared/rich-text-extensions";
import {
  RichTextToolbar,
  type RichTextVariable,
} from "@/components/shared/rich-text-toolbar";
import {
  RICH_TEXT_MUTED_CLASSES,
  sanitizeRichText,
  toRichTextHtml,
} from "@/components/shared/rich-text";
import { cn } from "@/lib/utils";

export type { RichTextVariable };

/** What TipTap writes for an empty document; stored as an empty string instead. */
const EMPTY_DOCUMENT = "<p></p>";

/** The content of a stored value: the editor's HTML or a converted markdown text, sanitized either way. */
function toContent(value: string | null) {
  return sanitizeRichText(toRichTextHtml(value ?? ""));
}

/**
 * A small rich text editor (TipTap): paragraphs, headings (levels 1 to 4), bold, italic, underline,
 * lists, tables, links and a few text colours. The value is HTML; render it with [RichText], which
 * sanitizes it. No code or block quotes: it is for notes (a tooltip hint, the body of a mail), not for
 * documents.
 *
 * A value that isn't the editor's HTML (plain text or markdown from before) is shown converted and
 * stored as HTML with the next change; markdown pasted as plain text is converted as well.
 *
 * [variables], if given, adds a menu inserting `{{key}}` at the cursor. [disabled] shows the text
 * without toolbar and read-only: a contenteditable is not disabled by a surrounding `<fieldset disabled>`.
 */
export function RichTextEditor({
  id,
  value,
  onChange,
  onBlur,
  placeholder,
  variables,
  invalid,
  disabled,
  className,
}: {
  id?: string;
  value: string | null;
  /** The HTML of the text, an empty string for an empty one. */
  onChange: (html: string) => void;
  onBlur?: () => void;
  placeholder?: string;
  variables?: RichTextVariable[];
  invalid?: boolean;
  disabled?: boolean;
  className?: string;
}) {
  // The last value written by the editor itself: a value coming back unchanged must not reset the
  // content (and the cursor); any other one (a reset form, a loaded entity) replaces it.
  const lastEmitted = useRef(value ?? "");
  // The editor keeps the callbacks of its creation; these refs hand it the current ones.
  const onChangeRef = useRef(onChange);
  const onBlurRef = useRef(onBlur);
  useEffect(() => {
    onChangeRef.current = onChange;
    onBlurRef.current = onBlur;
  });
  const editor = useEditor({
    // Static export: the editor is created in the browser only, never prerendered.
    immediatelyRender: false,
    extensions: richTextExtensions(placeholder ?? ""),
    content: toContent(value),
    editable: !disabled,
    onUpdate: ({ editor }) => {
      const html = editor.getHTML();
      lastEmitted.current = html === EMPTY_DOCUMENT ? "" : html;
      onChangeRef.current(lastEmitted.current);
    },
    onBlur: () => onBlurRef.current?.(),
    editorProps: {
      handlePaste: pasteMarkdown,
      attributes: {
        ...(id ? { id } : {}),
        class: cn(
          "rich-text-editor min-h-24 px-3 py-2 text-sm font-normal focus:outline-none",
          RICH_TEXT_MUTED_CLASSES
        ),
      },
    },
  });
  useEffect(() => {
    if (!editor || (value ?? "") === lastEmitted.current) return;
    lastEmitted.current = value ?? "";
    editor.commands.setContent(toContent(value), { emitUpdate: false });
  }, [value, editor]);
  useEffect(() => {
    editor?.setEditable(!disabled, false);
  }, [disabled, editor]);

  return (
    <div
      className={cn(
        "rounded-md border border-input bg-transparent shadow-xs focus-within:border-ring focus-within:ring-[3px] focus-within:ring-ring/50",
        invalid && "border-destructive",
        disabled && "opacity-50",
        className
      )}
    >
      {!disabled && <RichTextToolbar editor={editor} variables={variables} />}
      <EditorContent editor={editor} />
    </div>
  );
}
