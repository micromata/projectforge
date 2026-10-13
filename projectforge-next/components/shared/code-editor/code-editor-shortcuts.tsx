"use client";

import { Fragment } from "react";
import { useTranslations } from "next-intl";

/**
 * One key of a shortcut: `mod` is CTRL, on macOS CMD — the `Mod-` of CodeMirror's keymaps, which the
 * table below follows; `click` and `drag` are mouse gestures with the keys held.
 */
type Key = "mod" | "alt" | "shift" | "click" | "drag" | (string & {});

/** Alternatives, each a sequence of keys pressed together (`then` separates presses one after another). */
export type Shortcut = readonly (readonly Key[])[];

/**
 * The keys of CodeMirror's default keymaps (history, search, comment, line commands, `indentWithTab`)
 * the editor runs with, as they are bound there — this is documentation of those bindings, not a
 * binding of its own.
 */
export const CODE_EDITOR_SHORTCUTS = {
  undo: [["mod", "Z"]],
  redo: [["mod", "Y"]],
  search: [["mod", "F"]],
  nextMatch: [["F3"], ["mod", "G"]],
  previousMatch: [
    ["shift", "F3"],
    ["mod", "shift", "G"],
  ],
  toggleComment: [["mod", "/"]],
  indent: [["Tab"], ["mod", "]"]],
  unindent: [
    ["shift", "Tab"],
    ["mod", "["],
  ],
  leave: [["Esc", "then", "Tab"]],
  selectNextOccurrence: [["mod", "D"]],
  selectAllOccurrences: [["mod", "shift", "L"]],
  moveLine: [
    ["alt", "↑"],
    ["alt", "↓"],
  ],
  copyLine: [
    ["shift", "alt", "↑"],
    ["shift", "alt", "↓"],
  ],
  deleteLine: [["mod", "shift", "K"]],
  addCursor: [["mod", "click"]],
  rectangularSelection: [["alt", "drag"]],
} as const satisfies Record<string, Shortcut>;

export type CodeEditorCommand = keyof typeof CODE_EDITOR_SHORTCUTS;

/** On macOS redo is CMD-SHIFT-Z, as CodeMirror binds it there; CMD-Y is the history of Safari. */
const MAC_SHORTCUTS: Partial<Record<CodeEditorCommand, Shortcut>> = {
  redo: [["mod", "shift", "Z"]],
};

const isMac = () =>
  typeof navigator !== "undefined" &&
  /Mac|iPhone|iPad/.test(navigator.userAgent);

export function shortcutOf(command: CodeEditorCommand): Shortcut {
  return (isMac() && MAC_SHORTCUTS[command]) || CODE_EDITOR_SHORTCUTS[command];
}

/** The shortcut in the keys of the user's platform (`⌘ ⇧ Z` on macOS, `Strg Umschalt Z` elsewhere). */
export function ShortcutKeys({ shortcut }: { shortcut: Shortcut }) {
  const t = useTranslations("codeEditor.key");
  const mac = isMac();
  const label = (key: Key): string => {
    switch (key) {
      case "mod":
        return mac ? "⌘" : t("ctrl");
      case "alt":
        return mac ? "⌥" : "Alt";
      case "shift":
        return mac ? "⇧" : t("shift");
      case "click":
        return t("click");
      case "drag":
        return t("drag");
      case "then":
        return t("then");
      default:
        return key;
    }
  };
  return (
    <span className="inline-flex flex-wrap items-center gap-x-1.5 gap-y-1">
      {shortcut.map((keys, i) => (
        <Fragment key={i}>
          {i > 0 && <span className="text-muted-foreground">/</span>}
          <span className="inline-flex items-center gap-0.5">
            {keys.map((key, j) =>
              key === "then" ? (
                <span key={j} className="px-0.5">
                  {label(key)}
                </span>
              ) : (
                <kbd
                  key={j}
                  className="rounded border bg-muted px-1 font-mono text-[10.5px] leading-4 text-foreground"
                >
                  {label(key)}
                </kbd>
              )
            )}
          </span>
        </Fragment>
      ))}
    </span>
  );
}
