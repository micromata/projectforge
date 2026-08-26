"use client";

import { useTranslations } from "next-intl";
import { useEntityData } from "@/components/shared/form/form-context";
import { SelectField } from "@/components/shared/form/select-field";
import type { TimesheetDetail } from "../../types";

/**
 * The sheet's tag, chosen from the ones the installation configures (`ConfigurationParam.TIMESHEET_TAGS`).
 *
 * A select and not a free string: the legacy form offers a fixed list (`createTagUISelect`), and so does
 * this. Shown only where there is something to choose — the tags the server put on the DTO (see
 * `TimesheetDetail.tags`), which also carry the sheet's own tag on after it left the configuration. Where
 * none is configured the field is not rendered at all, exactly as the UILayout leaves it out.
 *
 * Optional, so the select keeps the ✕ that clears it (`!required` from the metadata, see SelectField).
 */
export function TagField({ className }: { className?: string }) {
  const t = useTranslations();
  const tags = useEntityData<TimesheetDetail>()?.tags;
  if (!tags || tags.length === 0) return null;

  return (
    <SelectField
      name="tag"
      label={t("timesheet.tag")}
      className={className}
      options={tags.map((tag) => ({ value: tag, label: tag }))}
    />
  );
}
