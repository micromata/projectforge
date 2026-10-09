"use client";

import { useTranslations } from "next-intl";
import { useStore } from "@tanstack/react-form";
import { CodeEditorField } from "@/components/shared/form/code-editor-field";
import { useEntityEditForm } from "@/components/shared/form/form-context";
import { cn } from "@/lib/utils";

/**
 * The code of the script, highlighted in the language of its type. The variables it has at hand are the
 * next section (ScriptAvailableVariablesField), its downloads sit beside the form's heading
 * (ScriptCodeDownloads).
 */
export function ScriptCodeField({ className }: { className?: string }) {
  const t = useTranslations();
  const form = useEntityEditForm();
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const type = useStore(form.store, (state: any) => state.values.type);
  return (
    <div className={cn("flex flex-col gap-3", className)}>
      <CodeEditorField
        name="script"
        label={t("scripting.script._")}
        language={type === "GROOVY" ? "groovy" : "kotlin"}
        minHeight="24rem"
        metadataLess
      />
    </div>
  );
}
