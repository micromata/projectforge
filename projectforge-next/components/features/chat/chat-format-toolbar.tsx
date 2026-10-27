"use client";

import { useEditorState, type Editor } from "@tiptap/react";
import { useTranslations } from "next-intl";
import { HugeiconsIcon, type IconSvgElement } from "@hugeicons/react";
import {
  CodeIcon,
  LeftToRightListBulletIcon,
  LeftToRightListNumberIcon,
  Link01Icon,
  SourceCodeSquareIcon,
  TextBoldIcon,
  TextItalicIcon,
  TextStrikethroughIcon,
  TextUnderlineIcon,
} from "@hugeicons/core-free-icons";
import { RichTextColorMenu } from "@/components/shared/rich-text-color-menu";
import {
  promptLink,
  ToolbarToggle,
} from "@/components/shared/rich-text-toolbar-controls";

type Format =
  | "bold"
  | "italic"
  | "underline"
  | "strike"
  | "bulletList"
  | "orderedList"
  | "code"
  | "codeBlock";

const FORMATS: {
  format: Format;
  icon: IconSvgElement;
  toggle: (editor: Editor) => void;
}[] = [
  {
    format: "bold",
    icon: TextBoldIcon,
    toggle: (e) => e.chain().focus().toggleBold().run(),
  },
  {
    format: "italic",
    icon: TextItalicIcon,
    toggle: (e) => e.chain().focus().toggleItalic().run(),
  },
  {
    format: "underline",
    icon: TextUnderlineIcon,
    toggle: (e) => e.chain().focus().toggleUnderline().run(),
  },
  {
    format: "strike",
    icon: TextStrikethroughIcon,
    toggle: (e) => e.chain().focus().toggleStrike().run(),
  },
  {
    format: "bulletList",
    icon: LeftToRightListBulletIcon,
    toggle: (e) => e.chain().focus().toggleBulletList().run(),
  },
  {
    format: "orderedList",
    icon: LeftToRightListNumberIcon,
    toggle: (e) => e.chain().focus().toggleOrderedList().run(),
  },
  {
    format: "code",
    icon: CodeIcon,
    toggle: (e) => e.chain().focus().toggleCode().run(),
  },
  {
    format: "codeBlock",
    icon: SourceCodeSquareIcon,
    toggle: (e) => e.chain().focus().toggleCodeBlock().run(),
  },
];

/**
 * The formatting of a chat message, a compact row above the editor: only what a message may contain
 * (see chatEditorExtensions and sanitizeChatHtml): no headings or tables as in RichTextToolbar, but code.
 */
export function ChatFormatToolbar({ editor }: { editor: Editor | null }) {
  const t = useTranslations("richTextEditor");
  const tChat = useTranslations("chat.message");
  const active = useEditorState({
    editor,
    selector: ({ editor }) => ({
      bold: editor?.isActive("bold") ?? false,
      italic: editor?.isActive("italic") ?? false,
      underline: editor?.isActive("underline") ?? false,
      strike: editor?.isActive("strike") ?? false,
      bulletList: editor?.isActive("bulletList") ?? false,
      orderedList: editor?.isActive("orderedList") ?? false,
      code: editor?.isActive("code") ?? false,
      codeBlock: editor?.isActive("codeBlock") ?? false,
      color: !!editor?.getAttributes("textStyle").color,
      link: editor?.isActive("link") ?? false,
    }),
  });

  const editLink = () => promptLink(editor, t("link.prompt"));

  return (
    <div
      className="flex flex-wrap items-center gap-0.5 border-b px-1 py-0.5"
      role="toolbar"
      aria-label={t("toolbar")}
    >
      {FORMATS.map(({ format, icon, toggle }) => (
        <ToolbarToggle
          key={format}
          label={t(format)}
          hint={format === "codeBlock" ? tChat("codeBlock") : undefined}
          pressed={active?.[format]}
          onPressedChange={() => editor && toggle(editor)}
        >
          <HugeiconsIcon icon={icon} />
        </ToolbarToggle>
      ))}
      <ToolbarToggle
        label={t("link._")}
        pressed={active?.link}
        onPressedChange={editLink}
      >
        <HugeiconsIcon icon={Link01Icon} />
      </ToolbarToggle>
      <RichTextColorMenu editor={editor} pressed={active?.color} />
    </div>
  );
}
