"use client";

import { useTranslations } from "next-intl";
import { CheckboxField } from "@/components/shared/form/checkbox-field";

/**
 * Whether saving notifies the contact person by e-mail — beside the save button, because it decides
 * what the save does rather than what the order is (`AuftragPagesRest.onAfterSaveOrUpdate`).
 *
 * The backend preselects it whenever the contact person is somebody else (see `toFormValues`); this is
 * where the user overrides that. Wicket puts the same checkbox into a fieldset of its own at the end of
 * the form, which is the one place a reader never looks before pressing save.
 */
export function SendNotificationOption() {
  const t = useTranslations();
  return (
    <CheckboxField
      name="sendEMailNotification"
      label={t("label.sendEMailNotification")}
    />
  );
}
