"use client";

import { useTranslations } from "next-intl";
import { CopyButton } from "@/components/shared/copy-button";
import { FormAlert } from "@/components/shared/form-alert";
import type { LogFileSearchData } from "@/lib/rs/admin-errors";

/**
 * Lines of the log file couldn't be read, so occurrences may be missing: names the format to configure, ready to
 * be copied into `projectforge.properties`, and the one configured now.
 */
export function LogFileFormatWarning({ data }: { data: LogFileSearchData }) {
  const t = useTranslations();
  if (!data.formatWarning) return null;
  const recommended = `logging.pattern.file=${data.recommendedPattern}`;
  return (
    <FormAlert tone="error">
      <p>
        {t("system.admin.adminErrors.logFile.formatWarning", {
          arg0: data.unparsedLines,
        })}
      </p>
      <div className="mt-2 flex items-start gap-2">
        <code className="flex-1 break-all rounded bg-background px-2 py-1 font-mono text-xs">
          {recommended}
        </code>
        <CopyButton value={recommended} label="logging.pattern.file" />
      </div>
      {data.pattern && data.pattern !== data.recommendedPattern && (
        <p className="mt-2 text-xs">
          {t("system.admin.adminErrors.logFile.configured")}{" "}
          <code className="break-all font-mono">{data.pattern}</code>
        </p>
      )}
    </FormAlert>
  );
}
