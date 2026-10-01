"use client";

import { useTranslations } from "next-intl";
import { InputField } from "@/components/shared/form/input-field";
import { useEntityData } from "@/components/shared/form/form-context";
import type { AddressCampaignDetail } from "./types";

/**
 * The campaign's values ("Value 1; Value 2; Value 3"), with the format as its ⓘ hint — and, on a saved
 * campaign, the warning of the former Wicket form (`AddressCampaignEditForm`'s alert icon) that relabelling a value may drop
 * the addresses already assigned to it: they store the value's text, not a reference to it.
 *
 * A custom field because the warning only applies once the campaign exists, which a static declaration
 * cannot tell; the field reads the loaded DTO for that (a new one has no id yet).
 */
export function AddressCampaignValuesField({
  className,
}: {
  className?: string;
}) {
  const t = useTranslations();
  const data = useEntityData<AddressCampaignDetail>();
  return (
    <InputField
      name="values"
      label={t("values")}
      hint={t("plugins.marketing.addressCampaign.values.format")}
      warning={
        data?.id != null
          ? t(
              "plugins.marketing.addressCampaign.edit.warning.doNotChangeValues"
            )
          : undefined
      }
      className={className}
    />
  );
}
