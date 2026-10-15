"use client";

import { useStore } from "@tanstack/react-form";
import { useTranslations } from "next-intl";
import { useEntityEditForm } from "@/components/shared/form/form-context";
import { SecretField } from "@/components/shared/form/secret-field";
import { cn } from "@/lib/utils";
import type { UserValues } from "./user-schema";

/**
 * The optional initial passwords of a new user (`UserEntityRest.onAfterSave` sets them). An existing
 * user's passwords are changed on their own pages, linked from the tokens section (see
 * UserAccessFields); the WLAN password only where it can be set at all.
 */
export function UserPasswordFields({ className }: { className?: string }) {
  const t = useTranslations();
  const form = useEntityEditForm();
  const { id, wlanPasswordSupported } = useStore(
    form.store,
    (s: unknown) => (s as FormState).values
  );
  if (id != null) return null;
  return (
    <div className={cn("grid gap-4 sm:grid-cols-2", className)}>
      <SecretField
        name="password"
        label={t("password._")}
        hint={t("user.add.optionalPassword")}
        metadataLess
      />
      {wlanPasswordSupported && (
        <SecretField
          name="wlanPassword"
          label={t("ldap.wlanSambaPassword")}
          hint={t("user.add.optionalPassword")}
          metadataLess
        />
      )}
    </div>
  );
}

/** The slice of the form store read here; the context is deliberately untyped (form-context). */
interface FormState {
  values: Pick<UserValues, "id" | "wlanPasswordSupported">;
}
