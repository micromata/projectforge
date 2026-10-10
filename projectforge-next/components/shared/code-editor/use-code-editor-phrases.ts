"use client";

import { useMemo } from "react";
import { useTranslations } from "next-intl";
import { EditorState, type Extension } from "@uiw/react-codemirror";

/**
 * The texts CodeMirror itself shows in its search panel, in the user's language, keyed by the English
 * phrase CodeMirror asks for (`$` is filled in by CodeMirror).
 */
export function useCodeEditorPhrases(): Extension {
  const t = useTranslations();
  return useMemo(
    () =>
      EditorState.phrases.of({
        Find: t("codeEditor.phrase.find"),
        Replace: t("codeEditor.phrase.replace"),
        next: t("codeEditor.phrase.next"),
        previous: t("codeEditor.phrase.previous"),
        all: t("codeEditor.phrase.all"),
        "match case": t("codeEditor.phrase.matchCase"),
        regexp: t("codeEditor.phrase.regexp"),
        "by word": t("codeEditor.phrase.byWord"),
        replace: t("codeEditor.phrase.replaceOne"),
        "replace all": t("codeEditor.phrase.replaceAll"),
        close: t("codeEditor.phrase.close"),
        "current match": t("codeEditor.phrase.currentMatch"),
        "on line": t("codeEditor.phrase.onLine"),
        "replaced match on line $": t("codeEditor.phrase.replacedOnLine"),
        "replaced $ matches": t("codeEditor.phrase.replacedMatches"),
      }),
    [t]
  );
}
