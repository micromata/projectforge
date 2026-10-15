"use client";

import { Fragment } from "react";
import { useTranslations } from "next-intl";
import {
  ShortcutKeys,
  shortcutOf,
  type CodeEditorCommand,
} from "./code-editor-shortcuts";

/** The order of the list: the commands of the toolbar first, then what only the keyboard does. */
const COMMANDS: readonly CodeEditorCommand[] = [
  "undo",
  "redo",
  "search",
  "nextMatch",
  "previousMatch",
  "toggleComment",
  "indent",
  "unindent",
  "leave",
  "selectNextOccurrence",
  "selectAllOccurrences",
  "moveLine",
  "copyLine",
  "deleteLine",
  "addCursor",
  "rectangularSelection",
];

/** All shortcuts of the editor, for the info button of its toolbar. */
export function CodeEditorShortcutList() {
  const t = useTranslations();
  // Spelled out: the i18n export of the generator finds literal keys only.
  const labels: Record<CodeEditorCommand, string> = {
    undo: t("codeEditor.undo"),
    redo: t("codeEditor.redo"),
    search: t("codeEditor.search"),
    nextMatch: t("codeEditor.nextMatch"),
    previousMatch: t("codeEditor.previousMatch"),
    toggleComment: t("codeEditor.toggleComment"),
    indent: t("codeEditor.indent"),
    unindent: t("codeEditor.unindent"),
    leave: t("codeEditor.leave"),
    selectNextOccurrence: t("codeEditor.selectNextOccurrence"),
    selectAllOccurrences: t("codeEditor.selectAllOccurrences"),
    moveLine: t("codeEditor.moveLine"),
    copyLine: t("codeEditor.copyLine"),
    deleteLine: t("codeEditor.deleteLine"),
    addCursor: t("codeEditor.addCursor"),
    rectangularSelection: t("codeEditor.rectangularSelection"),
  };
  return (
    <div className="space-y-2 text-xs">
      <h3 className="font-semibold">{t("codeEditor.shortcuts")}</h3>
      <dl className="grid grid-cols-[auto_auto] items-start gap-x-4 gap-y-1">
        {COMMANDS.map((command) => (
          <Fragment key={command}>
            <dt>{labels[command]}</dt>
            <dd>
              <ShortcutKeys shortcut={shortcutOf(command)} />
            </dd>
          </Fragment>
        ))}
      </dl>
    </div>
  );
}
