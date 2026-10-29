"use client";

import { useStore } from "@tanstack/react-form";
import { useTranslations } from "next-intl";
import { CheckboxField } from "@/components/shared/form/checkbox-field";
import { useEntityEditForm } from "@/components/shared/form/form-context";
import type { OrderNotificationRule } from "@/lib/rs/order-notification";
import { OrderNotificationButton } from "../order-notification-button";
import type { OrderValues } from "../order-schema";

/**
 * Whether saving notifies the contact person by e-mail — beside the save button, because it decides
 * what the save does rather than what the order is (`OrderEntityRest.onAfterSaveOrUpdate`).
 *
 * The backend preselects it whenever the contact person is somebody else (see `toFormValues`); this is
 * where the user overrides that. Wicket puts the same checkbox into a fieldset of its own at the end of
 * the form, which is the one place a reader never looks before pressing save.
 *
 * Beside it, the user's own notification settings (how *they* are notified about orders), which offer the
 * project and customer chosen in the form as exceptions to add.
 */
export function SendNotificationOption() {
  const t = useTranslations();
  const form = useEntityEditForm();
  const { project, customer } = useStore(
    form.store,
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    (state: any) => {
      const v = state.values as OrderValues;
      return { project: v.project, customer: v.customer };
    }
  );
  const suggestions: Pick<
    OrderNotificationRule,
    "type" | "id" | "displayName"
  >[] = [];
  if (project) {
    suggestions.push({ type: "PROJECT", ...project });
  }
  if (customer) {
    suggestions.push({ type: "CUSTOMER", ...customer });
  }
  return (
    <div className="flex items-center gap-2">
      <CheckboxField
        name="sendEMailNotification"
        // `Field` is `w-full`, which would push the button to the far right of the action bar.
        className="w-auto"
        label={t("label.sendEMailNotification")}
      />
      <OrderNotificationButton suggestions={suggestions} />
    </div>
  );
}
