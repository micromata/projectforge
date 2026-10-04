"use client";

import { useEffect, useRef } from "react";
import {
  renewDataTransferAccessToken,
  renewDataTransferPassword,
} from "@/lib/rs/datatransfer";
import type { EntityForm } from "@/components/shared/form/form-context";

/**
 * Fills in a missing access token and password the moment external access is switched on — the backend
 * refuses to save an external access without both (`DataTransferAreaEntityRest.validate`), and a new
 * area arrives without a password (it is never sent to the client unless stored). The legacy form did
 * the same on its watched checkboxes.
 *
 * An effect rather than a handler of the checkboxes, since either of two may switch it on, and only the
 * values matter, not which box was clicked. One attempt per opening: a failing call is not repeated in
 * a loop, the server's validation then says what is missing.
 */
export function useExternalSecrets(
  form: EntityForm,
  enabled: boolean,
  token: string | null,
  password: string | null
) {
  const attempted = useRef(false);
  useEffect(() => {
    if (!enabled) {
      attempted.current = false;
      return;
    }
    if (attempted.current || (token && password)) return;
    attempted.current = true;
    if (!token) {
      void renewDataTransferAccessToken().then((value) =>
        form.setFieldValue("externalAccessToken", value)
      );
    }
    if (!password) {
      void renewDataTransferPassword().then((value) =>
        form.setFieldValue("externalPassword", value)
      );
    }
  }, [form, enabled, token, password]);
}
