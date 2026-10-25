"use client";

import { useEditorState, type Editor } from "@tiptap/react";
import { HugeiconsIcon, type IconSvgElement } from "@hugeicons/react";
import {
  LeftToRightListBulletIcon,
  LeftToRightListNumberIcon,
  Link01Icon,
  TextBoldIcon,
  TextItalicIcon,
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
  | "bulletList"
  | "orderedList"
  | "link";

const MARKS: {
  mark: Mark;
  icon: IconSvgElement;
  toggle: (editor: Editor) => void;
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
    mark: "bulletList",
    icon: LeftToRightListBulletIcon,
    toggle: (e) => e.chain().focus().toggleBulletList().run(),
  },
  {
    mark: "orderedList",
    icon: LeftToRightListNumberIcon,
    toggle: (e) => e.chain().focus().toggleOrderedList().run(),
  },
];

/** The toolbar of [RichTextEditor]. */
export function RichTextToolbar({
  editor,
  variables,
}: {
  editor: Editor | null;
  variables?: RichTextVariable[];
}) {
  const t = useTranslations("richTextEditor");
  const active = useEditorState({
    editor,
    selector: ({ editor }) => ({
      bold: editor?.isActive("bold") ?? false,
      italic: editor?.isActive("italic") ?? false,
      underline: editor?.isActive("underline") ?? false,
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
      <HeadingMenu editor={editor} pressed={active?.heading} />
      {MARKS.map(({ mark, icon, toggle }) => (
        <ToolbarToggle
          key={mark}
          label={t(mark)}
          pressed={active?.[mark]}
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
      <TableMenu editor={editor} inTable={active?.table} />
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
