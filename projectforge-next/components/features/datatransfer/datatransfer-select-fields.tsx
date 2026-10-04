"use client";

import { useTranslations } from "next-intl";
import { SelectField } from "@/components/shared/form/select-field";
import type { DataTransferOption } from "@/lib/rs/datatransfer";
import { useDataTransferOptions } from "./use-datatransfer-options";

function toSelectOptions(options: DataTransferOption[] | undefined) {
  return (options ?? []).map((option) => ({
    value: String(option.id),
    label: option.displayName,
  }));
}

/**
 * How long a file stays before it is deleted automatically — one of the backend's fixed choices
 * (`DataTransferAreaDao.EXPIRY_DAYS_VALUES`), labelled by it ("7 days").
 */
export function DataTransferExpiryDaysField({
  className,
}: {
  className?: string;
}) {
  const t = useTranslations();
  return (
    <SelectField
      name="expiryDays"
      label={t("plugins.datatransfer.expiryDays._")}
      hint={t("plugins.datatransfer.expiryDays.info")}
      options={toSelectOptions(useDataTransferOptions()?.expiryDays)}
      valueType="number"
      clearable={false}
      className={className}
    />
  );
}

/**
 * The largest file that may be uploaded — the backend's fixed choices
 * (`DataTransferAreaDao.MAX_UPLOAD_SIZE_VALUES`, in KB), already limited to what Spring's multipart
 * configuration lets through.
 */
export function DataTransferMaxUploadSizeField({
  className,
}: {
  className?: string;
}) {
  const t = useTranslations();
  return (
    <SelectField
      name="maxUploadSizeKB"
      label={t("plugins.datatransfer.maxUploadSize._")}
      hint={t("plugins.datatransfer.maxUploadSize.info")}
      options={toSelectOptions(useDataTransferOptions()?.maxUploadSizes)}
      valueType="number"
      clearable={false}
      className={className}
    />
  );
}
