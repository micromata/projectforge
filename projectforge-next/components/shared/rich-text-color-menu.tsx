"use client";

import type { Editor } from "@tiptap/react";
import { PaintBoardIcon } from "@hugeicons/core-free-icons";
import { useTranslations } from "next-intl";
import { ToolbarMenu } from "@/components/shared/rich-text-toolbar-controls";
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

/** The text colour of the selection (extensions TextStyle and Color), for RichTextToolbar and the chat. */
export function RichTextColorMenu({
  editor,
  pressed,
}: {
  editor: Editor | null;
  pressed: boolean | undefined;
}) {
  const t = useTranslations("richTextEditor");
  return (
    <ToolbarMenu label={t("color._")} icon={PaintBoardIcon} pressed={pressed}>
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
  );
}
