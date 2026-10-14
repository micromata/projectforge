"use client";

import { useQuery } from "@tanstack/react-query";
import { useStore } from "@tanstack/react-form";
import { useTranslations } from "next-intl";
import { useEntityEditForm } from "@/components/shared/form/form-context";
import { InputField } from "@/components/shared/form/input-field";
import { ReadOnlyValue } from "@/components/shared/form/read-only-value";
import { SelectField } from "@/components/shared/form/select-field";
import { StringSuggestField } from "@/components/shared/form/string-suggest-field";
import {
  fetchTimeZoneSuggestions,
  TIME_ZONE_SUGGEST_QUERY_KEY,
} from "@/lib/rs/configuration";
import {
  fetchUserSettingsOptions,
  USER_SETTINGS_OPTIONS_QUERY_KEY,
} from "@/lib/rs/user";
import { cn } from "@/lib/utils";
import type { UserValues } from "./user-schema";

/**
 * The personal settings of the user — the same choices the my-account page offers
 * (`UserEntityRest.settingsOptions`, `UserSettingsOptions`) — and when the user last logged in and changed
 * the passwords. No locale is the default (the browser's), hence a clearable select.
 */
export function UserSettingsFields({ className }: { className?: string }) {
  const t = useTranslations();
  const form = useEntityEditForm();
  const values = useStore(form.store, (s: unknown) => (s as FormState).values);
  const { data: options } = useQuery({
    queryKey: USER_SETTINGS_OPTIONS_QUERY_KEY,
    queryFn: ({ signal }) => fetchUserSettingsOptions(signal),
    staleTime: Infinity,
  });
  return (
    <div className={cn("grid gap-4 sm:grid-cols-2 lg:grid-cols-3", className)}>
      <ReadOnlyValue label={t("login.lastLogin")}>
        {values.lastLoginFormatted}
      </ReadOnlyValue>
      <ReadOnlyValue label={t("user.changePassword.password.lastChange")}>
        {values.lastPasswordChangeFormatted}
      </ReadOnlyValue>
      {values.wlanPasswordSupported ? (
        <ReadOnlyValue label={t("user.changeWlanPassword.lastChange")}>
          {values.lastWlanPasswordChangeFormatted}
        </ReadOnlyValue>
      ) : (
        <div className="hidden lg:block" />
      )}
      <SelectField
        name="locale"
        label={t("user.locale")}
        hint={t("user.tooltip.locale")}
        options={options?.locales ?? []}
        placeholder={t("user.defaultLocale")}
      />
      <StringSuggestField
        name="timeZone"
        label={t("timezone")}
        hint={t("user.tooltip.timeZone")}
        suggest={fetchTimeZoneSuggestions}
        queryKey={TIME_ZONE_SUGGEST_QUERY_KEY}
      />
      <InputField
        name="personalPhoneIdentifiers"
        label={t("user.personalPhoneIdentifiers._")}
        hint={t("user.personalPhoneIdentifiers.tooltip.content")}
      />
      <SelectField
        name="dateFormat"
        label={t("dateFormat._")}
        hint={t("user.tooltip.dateFormat")}
        options={options?.dateFormats ?? []}
      />
      <SelectField
        name="excelDateFormat"
        label={t("dateFormat.xls")}
        hint={t("user.tooltip.excelDateFormat")}
        options={options?.excelDateFormats ?? []}
      />
      <SelectField
        name="timeNotation"
        label={t("timeNotation._")}
        hint={t("user.tooltip.timeNotation")}
        options={options?.timeNotations ?? []}
      />
    </div>
  );
}

/** The slice of the form store read here; the context is deliberately untyped (form-context). */
interface FormState {
  values: Pick<
    UserValues,
    | "lastLoginFormatted"
    | "lastPasswordChangeFormatted"
    | "lastWlanPasswordChangeFormatted"
    | "wlanPasswordSupported"
  >;
}
