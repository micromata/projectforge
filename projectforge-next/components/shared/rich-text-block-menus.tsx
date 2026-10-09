"use client";

import type { Editor } from "@tiptap/react";
import { HeadingIcon, Table01Icon } from "@hugeicons/core-free-icons";
import { useTranslations } from "next-intl";
import { RICH_TEXT_HEADING_LEVELS } from "@/components/shared/rich-text-extensions";
import { ToolbarMenu } from "@/components/shared/rich-text-toolbar-controls";
import {
  DropdownMenuItem,
  DropdownMenuSeparator,
} from "@/components/ui/dropdown-menu";

/** The menu of [RichTextToolbar] turning the current block into a paragraph or a heading. */
export function HeadingMenu({
  editor,
  pressed,
}: {
  editor: Editor | null;
  pressed: boolean | undefined;
}) {
  const t = useTranslations("richTextEditor");
  return (
    <ToolbarMenu label={t("textStyle")} icon={HeadingIcon} pressed={pressed}>
      <DropdownMenuItem
        onSelect={() => editor?.chain().focus().setParagraph().run()}
      >
        {t("paragraph")}
      </DropdownMenuItem>
      {RICH_TEXT_HEADING_LEVELS.map((level) => (
        <DropdownMenuItem
          key={level}
          onSelect={() => editor?.chain().focus().setHeading({ level }).run()}
        >
          {t(`heading${level}`)}
        </DropdownMenuItem>
      ))}
    </ToolbarMenu>
  );
}

type TableAction =
  | "addRowAfter"
  | "addColumnAfter"
  | "deleteRow"
  | "deleteColumn"
  | "toggleHeaderRow"
  | "deleteTable";

/** What the table menu offers inside a table; the last one (deleting it) set apart. */
const TABLE_ACTIONS: TableAction[] = [
  "addRowAfter",
  "addColumnAfter",
  "deleteRow",
  "deleteColumn",
  "toggleHeaderRow",
];

/**
 * The table menu of [RichTextToolbar]: outside a table it inserts one (three columns, with a header
 * row), inside it adds or removes rows and columns.
 */
export function TableMenu({
  editor,
  inTable,
}: {
  editor: Editor | null;
  inTable: boolean | undefined;
}) {
  const t = useTranslations("richTextEditor");
  const run = (action: TableAction) => editor?.chain().focus()[action]().run();
  return (
    <ToolbarMenu label={t("table._")} icon={Table01Icon} pressed={inTable}>
      {!inTable ? (
        <DropdownMenuItem
          onSelect={() =>
            editor
              ?.chain()
              .focus()
              .insertTable({ rows: 3, cols: 3, withHeaderRow: true })
              .run()
          }
        >
          {t("table.insert")}
        </DropdownMenuItem>
      ) : (
        <>
          {TABLE_ACTIONS.map((action) => (
            <DropdownMenuItem key={action} onSelect={() => run(action)}>
              {t(`table.${action}`)}
            </DropdownMenuItem>
          ))}
          <DropdownMenuSeparator />
          <DropdownMenuItem
            variant="destructive"
            onSelect={() => run("deleteTable")}
          >
            {t("table.deleteTable")}
          </DropdownMenuItem>
        </>
      )}
    </ToolbarMenu>
  );
}
