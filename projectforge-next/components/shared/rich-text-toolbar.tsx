"use client";

import { useEditorState, type Editor } from "@tiptap/react";
import { HugeiconsIcon, type IconSvgElement } from "@hugeicons/react";
import {
  LeftToRightListBulletIcon,
  LeftToRightListNumberIcon,
  Link01Icon,
  PaintBoardIcon,
  TextBoldIcon,
  TextItalicIcon,
  TextUnderlineIcon,
  VariableIcon,
} from "@hugeicons/core-free-icons";
import { useTranslations } from "next-intl";
import {
  HeadingMenu,
  TableMenu,
} from "@/components/shared/rich-text-block-menus";
import {
  ToolbarMenu,
  ToolbarToggle,
} from "@/components/shared/rich-text-toolbar-controls";
import { DropdownMenuItem } from "@/components/ui/dropdown-menu";
import { cn } from "@/lib/utils";

/**
 * The text colours offered. The value is written into the stored HTML (it must work in a mail as well,
 * so no CSS variable), the swatch shows the same colour.
 */
const COLORS = [
  { key: "red", value: "#dc2626", swatch: "bg-red-600" },
  { key: "green", value: "#16a34a", swatch: "bg-green-600" },
  { key: "blue", value: "#2563eb", swatch: "bg-blue-600" },
] as const;

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

  const editLink = () => {
    if (!editor) return;
    const previous = editor.getAttributes("link").href as string | undefined;
    const url = window.prompt(t("link.prompt"), previous ?? "https://")?.trim();
    if (url === undefined) return;
    const chain = editor.chain().focus().extendMarkRange("link");
    if (url === "" || url === "https://") {
      chain.unsetLink().run();
    } else {
      chain.setLink({ href: url }).run();
    }
  };

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
        label={t("link")}
        pressed={active?.link}
        onPressedChange={editLink}
      >
        <HugeiconsIcon icon={Link01Icon} />
      </ToolbarToggle>
      <ToolbarMenu
        label={t("color._")}
        icon={PaintBoardIcon}
        pressed={active?.color}
      >
        {COLORS.map((color) => (
          <DropdownMenuItem
            key={color.key}
            onSelect={() => editor?.chain().focus().setColor(color.value).run()}
          >
            <span
              className={cn("size-3 rounded-full", color.swatch)}
              aria-hidden
            />
            {t(`color.${color.key}`)}
          </DropdownMenuItem>
        ))}
        <DropdownMenuItem
          onSelect={() => editor?.chain().focus().unsetColor().run()}
        >
          <span className="size-3 rounded-full border" aria-hidden />
          {t("color.default")}
        </DropdownMenuItem>
      </ToolbarMenu>
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
