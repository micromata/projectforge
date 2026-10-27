"use client";

import { useEditorState, type Editor } from "@tiptap/react";
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
  VariableIcon,
} from "@hugeicons/core-free-icons";
import { useTranslations } from "next-intl";
import { RichTextColorMenu } from "@/components/shared/rich-text-color-menu";
import {
  HeadingMenu,
  TableMenu,
} from "@/components/shared/rich-text-block-menus";
import {
  promptLink,
  ToolbarMenu,
  ToolbarToggle,
} from "@/components/shared/rich-text-toolbar-controls";
import { EmojiPicker } from "@/components/shared/emoji/emoji-picker";
import type { RichTextFeatures } from "@/components/shared/rich-text-extensions";
import { NOTE_FEATURES } from "@/components/shared/rich-text-extensions";
import { DropdownMenuItem } from "@/components/ui/dropdown-menu";

/** A placeholder the caller resolves later, inserted as `{{key}}` (e.g. the fields of a mail). */
export interface RichTextVariable {
  key: string;
  label: string;
}

type Mark =
  | "bold"
  | "italic"
  | "underline"
  | "strike"
  | "bulletList"
  | "orderedList"
  | "code"
  | "codeBlock";

const MARKS: {
  mark: Mark;
  icon: IconSvgElement;
  toggle: (editor: Editor) => void;
  /** Offered with [RichTextFeatures.code] only. */
  code?: boolean;
}[] = [
  {
    mark: "bold",
    icon: TextBoldIcon,
    toggle: (e) => e.chain().focus().toggleBold().run(),
  },
  {
    mark: "italic",
    icon: TextItalicIcon,
    toggle: (e) => e.chain().focus().toggleItalic().run(),
  },
  {
    mark: "underline",
    icon: TextUnderlineIcon,
    toggle: (e) => e.chain().focus().toggleUnderline().run(),
  },
  {
    mark: "strike",
    icon: TextStrikethroughIcon,
    toggle: (e) => e.chain().focus().toggleStrike().run(),
    code: true,
  },
  {
    mark: "bulletList",
    icon: LeftToRightListBulletIcon,
    toggle: (e) => e.chain().focus().toggleBulletList().run(),
  },
  {
    mark: "orderedList",
    icon: LeftToRightListNumberIcon,
    toggle: (e) => e.chain().focus().toggleOrderedList().run(),
  },
  {
    mark: "code",
    icon: CodeIcon,
    toggle: (e) => e.chain().focus().toggleCode().run(),
    code: true,
  },
  {
    mark: "codeBlock",
    icon: SourceCodeSquareIcon,
    toggle: (e) => e.chain().focus().toggleCodeBlock().run(),
    code: true,
  },
];

/** The toolbar of [RichTextEditor] and of the chat editor: the groups of [features]. */
export function RichTextToolbar({
  editor,
  variables,
  features = NOTE_FEATURES,
  codeBlockHint,
}: {
  editor: Editor | null;
  variables?: RichTextVariable[];
  features?: RichTextFeatures;
  /** Explains the code block button, if the editor handles Enter differently in it (the chat). */
  codeBlockHint?: string;
}) {
  const t = useTranslations("richTextEditor");
  const tRoot = useTranslations();
  const active = useEditorState({
    editor,
    selector: ({ editor }) => ({
      bold: editor?.isActive("bold") ?? false,
      italic: editor?.isActive("italic") ?? false,
      underline: editor?.isActive("underline") ?? false,
      strike: editor?.isActive("strike") ?? false,
      code: editor?.isActive("code") ?? false,
      codeBlock: editor?.isActive("codeBlock") ?? false,
      bulletList: editor?.isActive("bulletList") ?? false,
      orderedList: editor?.isActive("orderedList") ?? false,
      link: editor?.isActive("link") ?? false,
      color: !!editor?.getAttributes("textStyle").color,
      heading: editor?.isActive("heading") ?? false,
      table: editor?.isActive("table") ?? false,
    }),
  });

  const editLink = () => promptLink(editor, t("link.prompt"));

  return (
    <div
      className="flex flex-wrap items-center gap-0.5 border-b px-1 py-1"
      role="toolbar"
      aria-label={t("toolbar")}
    >
      {features.headings && (
        <HeadingMenu editor={editor} pressed={active?.heading} />
      )}
      {MARKS.filter((m) => !m.code || features.code).map(
        ({ mark, icon, toggle }) => (
          <ToolbarToggle
            key={mark}
            label={t(mark)}
            hint={mark === "codeBlock" ? codeBlockHint : undefined}
            pressed={active?.[mark]}
            onPressedChange={() => editor && toggle(editor)}
          >
            <HugeiconsIcon icon={icon} />
          </ToolbarToggle>
        )
      )}
      <ToolbarToggle
        label={t("link._")}
        pressed={active?.link}
        onPressedChange={editLink}
      >
        <HugeiconsIcon icon={Link01Icon} />
      </ToolbarToggle>
      <RichTextColorMenu editor={editor} pressed={active?.color} />
      {features.tables && <TableMenu editor={editor} inTable={active?.table} />}
      {features.emoji && (
        <EmojiPicker
          ariaLabel={tRoot("chat.emoji.insert")}
          onPick={(emoji) => editor?.chain().focus().insertContent(emoji).run()}
        />
      )}
      {variables && variables.length > 0 && (
        <ToolbarMenu label={t("variable")} icon={VariableIcon}>
          {variables.map((variable) => (
            <DropdownMenuItem
              key={variable.key}
              onSelect={() =>
                editor
                  ?.chain()
                  .focus()
                  .insertContent(`{{${variable.key}}}`)
                  .run()
              }
            >
              {variable.label}
            </DropdownMenuItem>
          ))}
        </ToolbarMenu>
      )}
    </div>
  );
}
