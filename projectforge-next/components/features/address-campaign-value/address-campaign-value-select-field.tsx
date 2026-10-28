"use client";

import { useTranslations } from "next-intl";
import { SelectField } from "@/components/shared/form/select-field";
import { useEntityData } from "@/components/shared/form/form-context";
import type { AddressCampaignValueDetail } from "./types";

/**
 * The value, one of the values of the campaign ("Yes; No; Maybe", see AddressCampaignDO.valuesArray). A
 * custom field because the options are the loaded campaign's, which a static declaration cannot know.
 *
 * A value stored before the campaign's values were relabelled stays selectable, so opening and saving such an
 * entry does not silently clear it.
 */
export function AddressCampaignValueSelectField({
  className,
}: {
  className?: string;
}) {
  const t = useTranslations();
  const data = useEntityData<AddressCampaignValueDetail>();
  const values = data?.addressCampaign?.valuesArray ?? [];
  const options = values.map((value) => ({ value, label: value }));
  if (data?.value && !values.includes(data.value)) {
    options.push({ value: data.value, label: data.value });
  }
  return (
    <SelectField
      name="value"
      label={t("value")}
      options={options}
      clearable
      className={className}
    />
  );
}
