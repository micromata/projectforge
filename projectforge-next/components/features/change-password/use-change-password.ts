"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { changePassword } from "@/lib/rs/change-password";
import { RsError } from "@/lib/rs/client";
import { MY_ACCOUNT_QUERY_KEY } from "@/lib/rs/my-account";
import { toast } from "@/lib/toast";
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
 * The values, the field errors and the change of a change password page. On success the fields are emptied
 * and the date of the last change is reloaded, here and on the my-account page.
 */
export function useChangePassword(
  variant: PasswordVariant,
  init: ChangePasswordInit
) {
  const t = useTranslations();
  const queryClient = useQueryClient();
  const [values, setValues] = useState(EMPTY);
  const [errors, setErrors] = useState<FieldErrors>({});

  const change = useMutation({
    // The function holds the typed passwords: no cached mutation keeps them once the page is left.
    gcTime: 0,
    mutationFn: () =>
      changePassword(variant, { userId: init.userId, ...values }),
    onSuccess: (result) => {
      if (result.kind === "ok") {
        toast.success(t("user.changePassword.msg.passwordSuccessfullyChanged"));
        setValues(EMPTY);
        setErrors({});
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

  function setValue(name: ChangePasswordField, value: string) {
    setValues((v) => ({ ...v, [name]: value }));
    setErrors((e) => ({ ...e, [name]: undefined }));
  }

  return { values, errors, setValue, change };
}
