"use client";

import { useEffect, useRef } from "react";
import { useTheme } from "next-themes";
import {
  EditorView,
  getDefaultExtensions,
  type Extension,
} from "@uiw/react-codemirror";
import { MergeView, unifiedMergeView } from "@codemirror/merge";
import { useIsMobile } from "@/hooks/use-mobile";
import { cn } from "@/lib/utils";
import type { CodeEditorProps } from "./code-editor";
import { CODE_EDITOR_LANGUAGES } from "./code-editor-languages";
import { useCodeEditorPhrases } from "./use-code-editor-phrases";

export interface CodeDiffViewProps {
  /** The code as it was (stored), shown left resp. as the deleted lines. */
  original: string;
  /** The code as it is now, shown right resp. as the inserted lines. */
  current: string;
  language?: CodeEditorProps["language"];
  className?: string;
}

/** Unchanged stretches longer than this collapse, keeping a few lines of context around each change. */
const COLLAPSE_UNCHANGED = { margin: 3, minSize: 6 };

/**
 * A read-only diff of two versions of some code (CodeMirror's `@codemirror/merge`): side by side, on a
 * phone one column with the deleted and inserted lines inline. Looks like the [CodeEditor] (same theme,
 * syntax, line numbers) and is loaded only by [CodeDiffDialog], like the editor itself.
 *
 * Built imperatively — `@uiw/react-codemirror` wraps a single editor, a merge view is two — and rebuilt
 * whenever an input changes; it is shown for a moment and never edited, so nothing is lost by that.
 */
export default function CodeDiffView({
  original,
  current,
  language,
  className,
}: CodeDiffViewProps) {
  const host = useRef<HTMLDivElement>(null);
  const { resolvedTheme } = useTheme();
  const unified = useIsMobile();
  const phrases = useCodeEditorPhrases();
  useEffect(() => {
    const parent = host.current;
    if (!parent) return;
    const extensions: Extension[] = [
      getDefaultExtensions({
        theme: resolvedTheme === "dark" ? "dark" : "light",
        basicSetup: {
          foldGutter: false,
          highlightActiveLine: false,
          highlightActiveLineGutter: false,
        },
        indentWithTab: false,
        editable: false,
        readOnly: true,
      }),
      phrases,
      ...(language ? [CODE_EDITOR_LANGUAGES[language]] : []),
    ];
    const view = unified
      ? new EditorView({
          doc: current,
          parent,
          extensions: [
            extensions,
            unifiedMergeView({
              original,
              mergeControls: false,
              collapseUnchanged: COLLAPSE_UNCHANGED,
            }),
          ],
        })
      : new MergeView({
          a: { doc: original, extensions },
          b: { doc: current, extensions },
          parent,
          collapseUnchanged: COLLAPSE_UNCHANGED,
        });
    // The diff scrolls as a whole inside the host (both sides of a merge view together).
    view.dom.classList.add("h-full", "overflow-auto");
    return () => view.destroy();
  }, [original, current, language, resolvedTheme, unified, phrases]);
  return (
    <div
      ref={host}
      className={cn("overflow-hidden rounded-md border text-sm", className)}
    />
  );
}
