"use client";

import { useRouter } from "next/navigation";
import { useTranslations } from "next-intl";
import { Button } from "@/components/ui/button";
import { FieldGroup } from "@/components/ui/field";
import { FormActionBar } from "@/components/shared/form-action-bar";
import { FormAlert } from "@/components/shared/form-alert";
import { SectionCard } from "@/components/shared/section-card";
import { navigateInGesture } from "@/lib/navigate-in-gesture";
import { ReadOnlyValue } from "@/components/shared/form/read-only-value";
import { PasswordField, type PasswordProps } from "./change-password-fields";
import type {
  ChangePasswordField,
  ChangePasswordInit,
  PasswordVariant,
} from "./types";
import { useChangePassword } from "./use-change-password";

/**
 * The fields of a change password page. Plain `useState`, like the other standalone forms: no entity, and
 * every rule (the repeat, the password quality, the old password) is checked by the backend, which names the
 * field of each error. Shows when the password was last changed, refreshed after a successful change, and
 * a notice instead of the fields where this password can't be changed (see ChangePasswordInit.supported).
 */
export function ChangePasswordForm({
  variant,
  init,
}: {
  variant: PasswordVariant;
  init: ChangePasswordInit;
}) {
  const t = useTranslations();
  const router = useRouter();
  const { values, errors, setValue, change } = useChangePassword(variant, init);
  const wlan = variant === "wlan";
  // Offered for saving only as the user's own new login password: neither the WLAN password nor another
  // user's (an admin's change) must replace the saved login of the one at the keyboard.
  const newAutoComplete = !wlan && init.changeOwn ? "new-password" : "off";

  function field(
    name: ChangePasswordField,
    label: string,
    extra: Partial<PasswordProps> = {}
  ) {
    return (
      <PasswordField
        name={name}
        label={label}
        value={values[name]}
        error={errors[name]}
        onChange={(value) => setValue(name, value)}
        {...extra}
      />
    );
  }

  function cancel() {
    // An admin came from the user's edit page (React), the user from the my-account page. Opened
    // directly (new tab, bookmark), there is no way back: the start page then.
    if (init.changeOwn) navigateInGesture(router, "/myAccount");
    else if (window.history.length > 1) window.history.back();
    else navigateInGesture(router, "/");
  }

  return (
    <form
      className="flex min-h-0 flex-1 flex-col overflow-hidden"
      onSubmit={(event) => {
        event.preventDefault();
        if (!change.isPending) change.mutate();
      }}
    >
      {init.changeOwn && init.username && (
        // Tells the password manager whose password this is, so it updates that entry.
        <input
          type="text"
          name="username"
          autoComplete="username"
          value={init.username}
          readOnly
          hidden
        />
      )}
      <div className="flex-1 overflow-y-auto px-4 pb-6 pt-2">
        <SectionCard className="w-full max-w-xl">
          <FieldGroup>
            <ReadOnlyValue label={t("user.username")}>
              {init.userDisplayName}
            </ReadOnlyValue>
            <ReadOnlyValue
              label={
                wlan
                  ? t("user.changeWlanPassword.lastChange")
                  : t("user.changePassword.password.lastChange")
              }
            >
              {init.lastChangeFormatted}
            </ReadOnlyValue>
            {init.supported ? (
              <>
                {init.changeOwn &&
                  field(
                    "loginPassword",
                    wlan
                      ? t("user.changeWlanPassword.loginPassword")
                      : t("user.changePassword.oldPassword"),
                    { autoComplete: "current-password", autoFocus: true }
                  )}
                {field(
                  "newPassword",
                  wlan
                    ? t("user.changeWlanPassword.newPassword")
                    : t("user.changePassword.newPassword"),
                  {
                    hint: init.passwordQualityHint ?? undefined,
                    autoComplete: newAutoComplete,
                    autoFocus: !init.changeOwn,
                  }
                )}
                {field("passwordRepeat", t("passwordRepeat"), {
                  autoComplete: newAutoComplete,
                })}
              </>
            ) : (
              <FormAlert tone="info">
                {t("user.changePassword.notSupported")}
              </FormAlert>
            )}
          </FieldGroup>
        </SectionCard>
      </div>
      <FormActionBar>
        <Button type="button" variant="outline" onClick={cancel}>
          {t("cancel")}
        </Button>
        {init.supported && (
          <Button type="submit" disabled={change.isPending}>
            {t("update")}
          </Button>
        )}
      </FormActionBar>
    </form>
  );
}
