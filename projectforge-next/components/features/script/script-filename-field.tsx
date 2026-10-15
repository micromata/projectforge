"use client";

import { useTranslations } from "next-intl";
import { useEntityData } from "@/components/shared/form/form-context";
import { FieldHint } from "@/components/shared/form/field-hint";
import type { ScriptDetail } from "./types";

/**
 * The file of a script written by the classic pages, read only: such a file is moved into the
 * attachments on the next save (`ScriptEntityRest.onBeforeUpdate`). Nothing for every other script.
 */
export function ScriptFilenameField({ className }: { className?: string }) {
  const t = useTranslations();
  const filename = useEntityData<ScriptDetail>()?.filename;
  if (!filename) return null;
  return (
    <div className={className}>
      <div className="mb-1 flex items-center gap-1 text-sm font-medium">
        {t("file._")}
        <FieldHint
          hint={t("scripting.script.editForm.file.tooltip")}
          label={t("file._")}
        />
      </div>
      <div className="text-sm">{filename}</div>
    </div>
  );
}
