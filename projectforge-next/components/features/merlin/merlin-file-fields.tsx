"use client";

import { useTranslations } from "next-intl";
import { useStore } from "@tanstack/react-form";
import { CheckboxField } from "@/components/shared/form/checkbox-field";
import {
  useEntityData,
  useEntityEditForm,
} from "@/components/shared/form/form-context";
import { ReadOnlyValue } from "@/components/shared/form/read-only-value";
import type { MerlinDetail } from "./types";

/**
 * The Word® template in use (the latest docx of the attachments) and, if one is newer than the last change
 * of the variables, the Excel® template definition (the latest xlsx) — shown only, renewed by the
 * analysis after an upload.
 */
export function MerlinFileFields({ className }: { className?: string }) {
  const t = useTranslations();
  const form = useEntityEditForm();
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const word = useStore(form.store, (s: any) => s.values.wordTemplateFileName);
  const excel = useStore(
    form.store,
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    (s: any) => s.values.excelTemplateDefinitionFileName
  );
  return (
    <div className={className}>
      <ReadOnlyValue
        label={t("plugins.merlin.wordTemplateFile._")}
        hint={t("plugins.merlin.wordTemplateFile.info")}
      >
        {word}
      </ReadOnlyValue>
      {excel && (
        <ReadOnlyValue
          label={t("plugins.merlin.templateConfigurationFile._")}
          hint={t("plugins.merlin.templateConfigurationFile.info")}
        >
          {excel}
        </ReadOnlyValue>
      )}
    </div>
  );
}

/**
 * Whether the documents of a serial execution may be sent to the users' personal boxes — only offered where
 * the data transfer plugin is available.
 */
export function MerlinDataTransferUsageField({
  className,
}: {
  className?: string;
}) {
  const t = useTranslations();
  if (!useEntityData<MerlinDetail>()?.dataTransferAvailable) return null;
  return (
    <CheckboxField
      name="dataTransferUsage"
      label={t("plugins.merlin.template.dataTransferUsage._")}
      hint={t("plugins.merlin.template.dataTransferUsage.info")}
      className={className}
    />
  );
}
