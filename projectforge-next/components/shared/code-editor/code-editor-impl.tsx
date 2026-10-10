"use client";

import { useMemo, useRef } from "react";
import { useTheme } from "next-themes";
import CodeMirror, { type ReactCodeMirrorRef } from "@uiw/react-codemirror";
import { StreamLanguage } from "@codemirror/language";
import { search } from "@codemirror/search";
import { kotlin } from "@codemirror/legacy-modes/mode/clike";
import { groovy } from "@codemirror/legacy-modes/mode/groovy";
import type { CodeEditorProps } from "./code-editor";
import { CodeEditorToolbar } from "./code-editor-toolbar";
import { useCodeEditorPhrases } from "./use-code-editor-phrases";

const LANGUAGES = {
  kotlin: StreamLanguage.define(kotlin),
  groovy: StreamLanguage.define(groovy),
};

/**
 * The CodeMirror editor itself, loaded only by [CodeEditor] — CodeMirror and its language modes are a
 * few hundred kilobytes that no other page needs.
 *
 * Tab indents (`indentWithTab`, on by default in `@uiw/react-codemirror`); Escape, then Tab leaves the
 * editor, so it is no keyboard trap.
 */
export default function CodeEditorImpl({
  id,
  value,
  onChange,
  onBlur,
  language,
  readOnly,
  minHeight = "20rem",
  ariaLabel,
}: CodeEditorProps) {
  const { resolvedTheme } = useTheme();
  const editor = useRef<ReactCodeMirrorRef>(null);
  // The search panel at the top, under the toolbar, where it is seen in a long script. Configured here
  // rather than appended by `openSearchPanel` on first use: a reconfiguration would drop it again.
  const phrases = useCodeEditorPhrases();
  const extensions = useMemo(
    () => [
      search({ top: true }),
      phrases,
      ...(language ? [LANGUAGES[language]] : []),
    ],
    [language, phrases]
  );
  // Memoized like the extensions: `@uiw/react-codemirror` reconfigures the editor whenever either is a
  // new object, which closes an open search panel at the next re-render (a blur marks the field touched).
  const basicSetup = useMemo(
    () => ({ foldGutter: false, highlightActiveLine: !readOnly }),
    [readOnly]
  );
  return (
    <div className="overflow-hidden rounded-md border text-sm">
      <CodeEditorToolbar
        view={() => editor.current?.view}
        readOnly={readOnly}
      />
      <CodeMirror
        id={id}
        value={value}
        onChange={onChange}
        onBlur={onBlur}
        ref={editor}
        extensions={extensions}
        readOnly={readOnly}
        editable={!readOnly}
        theme={resolvedTheme === "dark" ? "dark" : "light"}
        minHeight={minHeight}
        aria-label={ariaLabel}
        basicSetup={basicSetup}
      />
    </div>
  );
}
