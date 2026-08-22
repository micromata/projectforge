"use client";

import { useTranslations } from "next-intl";
import { InputField } from "@/components/shared/form/input-field";

/**
 * The mail addresses of every member, comma separated — the read-only field the legacy form ends with
 * (`UIReadOnlyField("emails")`), there to be copied into a mail client.
 *
 * Computed on read (`Group.populateEmails`) and therefore not a property of GroupDO: no metadata
 * describes it, which is why this is a custom field and not a declared one. It only follows the
 * *saved* members — a user just picked shows up after the save, as in the legacy form.
 */
export function MemberEmailsField({ className }: { className?: string }) {
  const t = useTranslations();
  return (
    <InputField
      name="emails"
      label={t("address.emails")}
      disabled
      metadataLess
      className={className}
    />
  );
}
