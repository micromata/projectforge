"use client";

import { useMemo } from "react";
import { useTranslations } from "next-intl";
import { useStore } from "@tanstack/react-form";
import { CodeEditorField } from "@/components/shared/form/code-editor-field";
import type { CodeVersionSource } from "@/components/shared/code-editor/types";
import {
  useEntityData,
  useEntityEditForm,
} from "@/components/shared/form/form-context";
import { fetchScriptBackup, fetchScriptBackups } from "@/lib/rs/script";
import { cn } from "@/lib/utils";

/**
 * The code of the script, highlighted in the language of its type, as tall as the form's column: the
 * code scrolls in the editor, its toolbar stays in view. The variables it has at hand are the
 * next section (ScriptAvailableVariablesField), its downloads sit beside the form's heading
 * (ScriptCodeDownloads). Its diff compares the backups as well (ScriptDao.getBackups).
 */
export function ScriptCodeField({ className }: { className?: string }) {
  const t = useTranslations();
  const form = useEntityEditForm();
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const type = useStore(form.store, (state: any) => state.values.type);
  const id = useEntityData<{ id?: number | null }>()?.id;
  const versions = useMemo<CodeVersionSource | undefined>(
    () =>
      id == null
        ? undefined
        : {
            queryKey: ["script", id, "backups"],
            list: (signal) => fetchScriptBackups(id, signal),
            load: (key, signal) => fetchScriptBackup(id, key, signal),
          },
    [id]
  );
  return (
    <div className={cn("flex flex-col gap-3", className)}>
      <CodeEditorField
        name="script"
        label={t("scripting.script._")}
        language={type === "GROOVY" ? "groovy" : "kotlin"}
        minHeight="24rem"
        fill
        hideLabel
        metadataLess
        showDiff
        versions={versions}
      />
    </div>
  );
}
