"use client";

import { useMutation, useQuery } from "@tanstack/react-query";
import { useStore } from "@tanstack/react-form";
import { useTranslations } from "next-intl";
import { Button } from "@/components/ui/button";
import { CheckboxField } from "@/components/shared/form/checkbox-field";
import { useEntityEditForm } from "@/components/shared/form/form-context";
import { InputField } from "@/components/shared/form/input-field";
import { NumberField } from "@/components/shared/form/number-field";
import { ReadOnlyValue } from "@/components/shared/form/read-only-value";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import { leafKeyOf } from "@/lib/leaf-key";
import {
  fetchUserLdapDefaults,
  fetchUserSettingsOptions,
  USER_SETTINGS_OPTIONS_QUERY_KEY,
} from "@/lib/rs/user";
import { toast } from "@/lib/toast";
import { cn } from "@/lib/utils";
import type { UserValues } from "./user-schema";

/**
 * The LDAP part of the user form, shown where posix or samba accounts are in use (the card's `visible`,
 * see USER_PAGE): the local user flag, the posix account (uid, gid, home, shell) and the samba account
 * (SID, primary group SID, the NT password hash read-only). A uid or SID still missing can be proposed by
 * the backend (`createUid`, `createSambaSID`), as the legacy form's "create" buttons did.
 */
export function UserLdapFields({ className }: { className?: string }) {
  const t = useTranslations();
  const form = useEntityEditForm();
  const values = useStore(form.store, (s: unknown) => (s as FormState).values);
  const { data: options } = useQuery({
    queryKey: USER_SETTINGS_OPTIONS_QUERY_KEY,
    queryFn: ({ signal }) => fetchUserSettingsOptions(signal),
    staleTime: Infinity,
  });
  const create = useMutation({
    // The whole form goes with the request; only the LDAP values of the answer are taken.
    mutationFn: (kind: "createUid" | "createSambaSID") =>
      fetchUserLdapDefaults(kind, form.state.values),
    onSuccess: (ldap) => {
      if (!ldap) return;
      const current = (form.state as FormState).values.ldapValues;
      form.setFieldValue(
        "ldapValues" as never,
        { ...current, ...ldap } as never
      );
    },
    onError: (error: unknown) =>
      toast.error(error instanceof Error ? error.message : String(error)),
  });
  const { ldapValues, posixConfigured, sambaConfigured } = values;
  const sambaLabel = `${t("ldap.sambaAccount")}: ${options?.sambaSIDPrefix ?? ""}-`;
  const createButton = (kind: "createUid" | "createSambaSID") => (
    <HintTooltip
      text={
        kind === "createUid"
          ? t("ldap.uidNumber.createDefault.tooltip")
          : t("ldap.sambaSID.createDefault.tooltip")
      }
    >
      <Button
        type="button"
        variant="outline"
        size="sm"
        disabled={create.isPending}
        onClick={() => create.mutate(kind)}
      >
        {t("create")}
      </Button>
    </HintTooltip>
  );
  return (
    <div className={cn("flex flex-col gap-4", className)}>
      <CheckboxField
        name="localUser"
        label={t("user.localUser._")}
        hint={t("user.localUser.tooltip")}
      />
      <div className="grid gap-4 sm:grid-cols-2">
        {posixConfigured && (
          <>
            <div className="flex min-w-0 flex-wrap items-end gap-3">
              <NumberField
                name="ldapValues.uidNumber"
                label={t(leafKeyOf("ldap.uidNumber", t.has))}
                hint={t("ldap.uidNumber.tooltip")}
                additionalLabel={t("ldap.posixAccount")}
                metadataLess
                maxDigits={7}
              />
              {ldapValues.uidNumber == null && createButton("createUid")}
            </div>
            <NumberField
              name="ldapValues.gidNumber"
              label={t(leafKeyOf("ldap.gidNumber", t.has))}
              hint={t("ldap.user.gidNumber.tooltip")}
              additionalLabel={t("ldap.posixAccount")}
              metadataLess
              maxDigits={7}
            />
            <InputField
              name="ldapValues.homeDirectory"
              label={t("ldap.homeDirectory")}
              metadataLess
            />
            <InputField
              name="ldapValues.loginShell"
              label={t("ldap.loginShell")}
              metadataLess
            />
          </>
        )}
        {sambaConfigured && (
          <>
            <div className="flex min-w-0 flex-wrap items-end gap-3">
              <NumberField
                name="ldapValues.sambaSIDNumber"
                label={t(leafKeyOf("ldap.sambaSID", t.has))}
                hint={t("ldap.sambaSID.tooltip")}
                additionalLabel={sambaLabel}
                metadataLess
                maxDigits={7}
              />
              {ldapValues.sambaSIDNumber == null &&
                createButton("createSambaSID")}
            </div>
            <NumberField
              name="ldapValues.sambaPrimaryGroupSIDNumber"
              label={t(leafKeyOf("ldap.sambaPrimaryGroupSID", t.has))}
              hint={t("ldap.sambaPrimaryGroupSID.tooltip")}
              additionalLabel={sambaLabel}
              metadataLess
              maxDigits={7}
            />
            <ReadOnlyValue
              label={t(leafKeyOf("ldap.sambaNTPassword", t.has))}
              hint={`${t("ldap.sambaNTPassword.subtitle")}: ${t("ldap.sambaNTPassword.tooltip")}`}
            >
              {ldapValues.sambaNTPassword}
            </ReadOnlyValue>
          </>
        )}
      </div>
    </div>
  );
}

/** The slice of the form store read here; the context is deliberately untyped (form-context). */
interface FormState {
  values: Pick<
    UserValues,
    "ldapValues" | "posixConfigured" | "sambaConfigured"
  >;
}
