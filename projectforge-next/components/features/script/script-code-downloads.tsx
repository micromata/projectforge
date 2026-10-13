"use client";

import { useTranslations } from "next-intl";
import { useMutation } from "@tanstack/react-query";
import { ExportButton } from "@/components/shared/export-button";
import {
  downloadEffectiveScript,
  downloadScriptBackups,
} from "@/lib/rs/script";
import { toast } from "@/lib/toast";

/**
 * The two downloads of a stored script's code, beside the heading of its form: the former versions (a
 * ZIP) and the code as executed, its includes resolved. Both are built from the database, so a new
 * script has neither.
 */
export function ScriptCodeDownloads({ id }: { id?: number | null }) {
  const t = useTranslations();
  const onError = (error: unknown) =>
    toast.error(error instanceof Error ? error.message : String(error));
  const backups = useMutation({
    mutationFn: (scriptId: number) => downloadScriptBackups(scriptId),
    onError,
  });
  const effective = useMutation({
    mutationFn: (scriptId: number) => downloadEffectiveScript(scriptId),
    onError,
  });
  if (id == null) return null;
  return (
    <>
      <ExportButton
        label={t("scripting.script.downloadBackups")}
        isPending={backups.isPending}
        onClick={() => backups.mutate(id)}
      />
      <ExportButton
        label={t("scripting.script.downloadEffectiveScript._")}
        tooltip={t("scripting.script.downloadEffectiveScript.info")}
        isPending={effective.isPending}
        onClick={() => effective.mutate(id)}
      />
    </>
  );
}
