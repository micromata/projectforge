"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { useTranslations } from "next-intl";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { Button } from "@/components/ui/button";
import { FieldGroup } from "@/components/ui/field";
import { SectionCard } from "@/components/shared/section-card";
import { navigateInGesture } from "@/lib/navigate-in-gesture";
import { changePassword } from "@/lib/rs/change-password";
import { RsError } from "@/lib/rs/client";
import { MY_ACCOUNT_QUERY_KEY } from "@/lib/rs/my-account";
import { toast } from "@/lib/toast";
import {
  PasswordField,
  ReadOnlyValue,
  type PasswordProps,
} from "./change-password-fields";
import { splitErrors, type FieldErrors } from "./errors";
import type {
  ChangePasswordField,
  ChangePasswordInit,
  PasswordVariant,
} from "./types";

const EMPTY: Record<ChangePasswordField, string> = {
  loginPassword: "",
  newPassword: "",
  passwordRepeat: "",
};

/**
 * The fields of a change password page. Plain `useState`, like the other standalone forms: no entity, and
 * every rule (the repeat, the password quality, the old password) is checked by the backend, which names the
 * field of each error. Shows when the password was last changed, refreshed after a successful change.
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
  const queryClient = useQueryClient();
  const [values, setValues] = useState(EMPTY);
  const [errors, setErrors] = useState<FieldErrors>({});
  const wlan = variant === "wlan";

  const change = useMutation({
    mutationFn: () =>
      changePassword(variant, { userId: init.userId, ...values }),
    onSuccess: (result) => {
      if (result.kind === "ok") {
        toast.success(t("user.changePassword.msg.passwordSuccessfullyChanged"));
        setValues(EMPTY);
        setErrors({});
        // The date of the last change, here and on the my-account page.
        void queryClient.invalidateQueries({
          queryKey: ["changePassword", variant],
        });
        if (init.changeOwn)
          void queryClient.invalidateQueries({
            queryKey: MY_ACCOUNT_QUERY_KEY,
          });
      } else if (result.kind === "rejected") {
        toast.error(result.message);
      } else {
        const { fieldErrors, unassigned } = splitErrors(
          result.validationErrors
        );
        unassigned.forEach((message) => toast.error(message));
        setErrors(fieldErrors);
      }
    },
    onError: (error) =>
      toast.error(error instanceof RsError ? error.message : String(error)),
  });

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
        onChange={(value) => {
          setValues((v) => ({ ...v, [name]: value }));
          setErrors((e) => ({ ...e, [name]: undefined }));
        }}
        {...extra}
      />
    );
  }

  function cancel() {
    // An admin came from the user's edit page (React), the user from the my-account page.
    if (init.changeOwn) navigateInGesture(router, "/myAccount");
    else window.history.back();
  }

  return (
    <SectionCard className="w-full max-w-xl">
      <form
        onSubmit={(event) => {
          event.preventDefault();
          if (!change.isPending) change.mutate();
        }}
      >
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
              // The WLAN password isn't the login: the browser mustn't store it as such.
              autoComplete: wlan ? "off" : "new-password",
              autoFocus: !init.changeOwn,
            }
          )}
          {field("passwordRepeat", t("passwordRepeat"), {
            autoComplete: wlan ? "off" : "new-password",
          })}
          <div className="flex items-center gap-3">
            <Button type="button" variant="outline" onClick={cancel}>
              {t("cancel")}
            </Button>
            <Button type="submit" disabled={change.isPending}>
              {t("update")}
            </Button>
          </div>
        </FieldGroup>
      </form>
    </SectionCard>
  );
}
