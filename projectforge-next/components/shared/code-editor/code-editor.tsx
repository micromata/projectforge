"use client";

import { lazy, Suspense } from "react";
import { Spinner } from "@/components/shared/spinner";

const CodeEditorImpl = lazy(() => import("./code-editor-impl"));

export interface CodeEditorProps {
  id?: string;
  value: string;
  onChange?: (value: string) => void;
  onBlur?: () => void;
  /** The syntax to highlight; plain text without one. */
  language?: "kotlin" | "groovy";
  readOnly?: boolean;
  /** CSS height the editor starts with; it grows with its content. */
  minHeight?: string;
  /** Accessible name, where no label points at the editor. */
  ariaLabel?: string;
}

/**
 * An editor for source code (CodeMirror 6): line numbers, syntax highlighting, bracket matching, search
 * and undo — what a textarea lacks for a script of some hundred lines. Loaded on first use, so the pages
 * without one don't carry it.
 */
export function CodeEditor(props: CodeEditorProps) {
  return (
    <Suspense
      fallback={
        <div className="flex min-h-40 items-center justify-center rounded-md border">
          <Spinner className="h-5 w-5 border-2" />
        </div>
      }
    >
      <CodeEditorImpl {...props} />
    </Suspense>
  );
}
