"use client";

import { useMemo } from "react";
import { useTheme } from "next-themes";
import CodeMirror from "@uiw/react-codemirror";
import { StreamLanguage } from "@codemirror/language";
import { kotlin } from "@codemirror/legacy-modes/mode/clike";
import { groovy } from "@codemirror/legacy-modes/mode/groovy";
import type { CodeEditorProps } from "./code-editor";

const LANGUAGES = {
  kotlin: StreamLanguage.define(kotlin),
  groovy: StreamLanguage.define(groovy),
};

/**
 * The CodeMirror editor itself, loaded only by [CodeEditor] — CodeMirror and its language modes are a
 * few hundred kilobytes that no other page needs.
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
  const extensions = useMemo(
    () => (language ? [LANGUAGES[language]] : []),
    [language]
  );
  return (
    <CodeMirror
      id={id}
      value={value}
      onChange={onChange}
      onBlur={onBlur}
      extensions={extensions}
      readOnly={readOnly}
      editable={!readOnly}
      theme={resolvedTheme === "dark" ? "dark" : "light"}
      minHeight={minHeight}
      aria-label={ariaLabel}
      basicSetup={{ foldGutter: false, highlightActiveLine: !readOnly }}
      className="overflow-hidden rounded-md border text-sm"
    />
  );
}
