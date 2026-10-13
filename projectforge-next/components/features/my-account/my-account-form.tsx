"use client";

import type { ReactNode } from "react";
import { useTranslations } from "next-intl";
import { useQueryClient } from "@tanstack/react-query";
import { EditPageShell } from "@/components/shared/edit-page-shell";
import type { EditPageTab } from "@/components/shared/edit-page-tabs";
import { EntityEditActions } from "@/components/shared/edit/entity-edit-actions";
import { FieldShell, useFieldIds } from "@/components/shared/form/field-shell";
import { EntityEditFormProvider } from "@/components/shared/form/form-context";
import { InputField } from "@/components/shared/form/input-field";
import { SelectField } from "@/components/shared/form/select-field";
import { StringSuggestField } from "@/components/shared/form/string-suggest-field";
import { TextAreaField } from "@/components/shared/form/text-area-field";
import { SectionCard } from "@/components/shared/section-card";
import { SectionHeader } from "@/components/shared/section-header";
import { useEntityEditForm } from "@/hooks/use-entity-edit-form";
import { useSubmitShortcut } from "@/hooks/use-submit-shortcut";
import { useUnsavedChangesWarning } from "@/hooks/use-unsaved-changes-warning";
import { resolveMenuUrl, toAbsoluteUrl } from "@/lib/menu-url";
import {
  fetchTimeZoneSuggestions,
  TIME_ZONE_SUGGEST_QUERY_KEY,
} from "@/lib/rs/configuration";
import { MY_ACCOUNT_QUERY_KEY, saveMyAccount } from "@/lib/rs/my-account";
import { MyAccountDevices } from "./my-account-devices";
import { MyAccountTokens } from "./my-account-tokens";
import {
  EMPTY_VALUES,
  MY_ACCOUNT_FIELDS,
  MY_ACCOUNT_METADATA,
  myAccountSchema,
  toFormValues,
  type MyAccountValues,
} from "./schema";
import type { MyAccountData, MyAccountInit } from "./types";

const toInitFormValues = (init: MyAccountInit) => toFormValues(init.data);

/**
 * The form of the my-account page: the user's own data and settings in one save, the tokens and devices
 * beside it with their own immediate actions (renew, log out), which change nothing the form holds.
 *
 * Saving stays on the page. Locale, time zone and formats take effect at once: the refetched user status
 * carries them to the whole app (see useAuth).
 */
export function MyAccountForm({ init }: { init: MyAccountInit }) {
  const t = useTranslations();
  const queryClient = useQueryClient();
  const { form, isDirty, isSubmitting } = useEntityEditForm<
    MyAccountValues,
    MyAccountInit
  >({
    data: init,
    toFormValues: toInitFormValues,
    defaultValues: EMPTY_VALUES,
    schema: myAccountSchema,
    fieldNames: MY_ACCOUNT_FIELDS,
    listRoute: "/myAccount",
    savedMessage: t("user.myAccount.saved"),
    onSaved: () => {
      void queryClient.invalidateQueries({ queryKey: MY_ACCOUNT_QUERY_KEY });
      void queryClient.invalidateQueries({ queryKey: ["userStatus"] });
    },
    save: saveMyAccount,
  });
  useUnsavedChangesWarning(isDirty);
  const onKeyDown = useSubmitShortcut(
    () => void form.handleSubmit(),
    isDirty && !isSubmitting
  );
  const { data, options, defaults } = init;
  const readOnly = !init.writeAccess;

  const cards: { id: string; label: string; content: ReactNode }[] = [
    {
      id: "account",
      label: t("user.myAccount.section.account"),
      content: <AccountSection init={init} readOnly={readOnly} />,
    },
    {
      id: "settings",
      label: t("user.myAccount.section.settings"),
      content: (
        <div className="grid gap-4 sm:grid-cols-2">
          <SelectField
            disabled={readOnly}
            name="locale"
            label={t("user.locale")}
            hint={t("user.tooltip.locale")}
            options={options.locales}
            clearable={false}
          />
          <StringSuggestField
            disabled={readOnly}
            name="timeZone"
            label={t("timezone")}
            hint={t("user.tooltip.timeZone")}
            suggest={fetchTimeZoneSuggestions}
            queryKey={TIME_ZONE_SUGGEST_QUERY_KEY}
          />
          <SelectField
            disabled={readOnly}
            name="dateFormat"
            label={t("dateFormat._")}
            hint={t("user.tooltip.dateFormat")}
            options={options.dateFormats}
            placeholder={t("user.myAccount.defaultValue", {
              arg0: defaults.dateFormat,
            })}
          />
          <SelectField
            disabled={readOnly}
            name="excelDateFormat"
            label={t("dateFormat.xls")}
            hint={t("user.tooltip.excelDateFormat")}
            options={options.excelDateFormats}
            placeholder={t("user.myAccount.defaultValue", {
              arg0: defaults.excelDateFormat,
            })}
          />
          <SelectField
            disabled={readOnly}
            name="timeNotation"
            label={t("timeNotation._")}
            hint={t("user.tooltip.timeNotation")}
            options={options.timeNotations}
            placeholder={
              defaults.timeNotation
                ? t("user.myAccount.defaultValue", {
                    arg0: defaults.timeNotation,
                  })
                : undefined
            }
          />
          {/* Only of use for the direct calls of Sipgate (SipgateDirectCallService). */}
          {init.phoneIdentifiersSupported && (
            <InputField
              disabled={readOnly}
              name="personalPhoneIdentifiers"
              label={t("user.personalPhoneIdentifiers._")}
              hint={t("user.personalPhoneIdentifiers.tooltip.content")}
            />
          )}
        </div>
      ),
    },
    {
      id: "tokens",
      label: t("user.myAccount.section.tokens"),
      content: (
        <MyAccountTokens tokens={data.tokens ?? []} canRenew={!readOnly} />
      ),
    },
    {
      id: "devices",
      label: t("login.stayLoggedIn.devices._"),
      content: (
        <MyAccountDevices devices={data.devices ?? []} canLogout={!readOnly} />
      ),
    },
    {
      id: "publicKeys",
      label: t("user.myAccount.section.publicKeys"),
      content: (
        // A key is one long word (base64): without `min-w-0` the auto-sizing textarea would take the width
        // of the whole key and push the page sideways.
        <div className="grid grid-cols-[minmax(0,1fr)] gap-4">
          <TextAreaField
            disabled={readOnly}
            name="gpgPublicKey"
            label={t("user.gpgPublicKey")}
            hint={t("user.tooltip.gpgPublicKey")}
            rows={4}
            className="min-w-0"
            controlClassName="max-h-64 overflow-y-auto"
          />
          <TextAreaField
            disabled={readOnly}
            name="sshPublicKey"
            label={t("user.sshPublicKey")}
            hint={t("user.tooltip.sshPublicKey")}
            rows={4}
            className="min-w-0"
            controlClassName="max-h-64 overflow-y-auto"
          />
        </div>
      ),
    },
  ];
  const tabs: EditPageTab[] = cards.map(({ id, label }) => ({ id, label }));
  const sections = cards.map(({ id, label, content }) => (
    <SectionCard key={id}>
      <SectionHeader title={label} />
      {content}
    </SectionCard>
  ));

  return (
    <EntityEditFormProvider
      value={{ form, metadata: MY_ACCOUNT_METADATA, readOnly, data }}
    >
      <form
        className="flex min-h-0 flex-1 flex-col overflow-hidden"
        onKeyDown={onKeyDown}
        onSubmit={(event) => {
          event.preventDefault();
          void form.handleSubmit();
        }}
      >
        <EditPageShell
          header={null}
          tabs={tabs}
          sections={sections}
          actions={
            <EntityEditActions
              // There is no list to go back to: cancelling takes back the changes since loading or the
              // last save.
              onCancel={() => form.reset()}
              // A restricted or demo user may not change the account (see MyAccountRest.InitData).
              canSave={!readOnly}
              isSaving={isSubmitting}
              isDirty={isDirty}
              allowSaveUnchanged={false}
              lastSaved={null}
            />
          }
        />
      </form>
    </EntityEditFormProvider>
  );
}

/** Who the user is (read-only, administered elsewhere) and the editable name, plus the password links. */
function AccountSection({
  init,
  readOnly,
}: {
  init: MyAccountInit;
  readOnly: boolean;
}) {
  const t = useTranslations();
  const data: MyAccountData = init.data;
  return (
    <div className="flex flex-col gap-4">
      <div className="grid gap-4 sm:grid-cols-2">
        <ReadOnlyValue
          label={t("user.username")}
          hint={t("user.tooltip.username")}
        >
          {data.username}
        </ReadOnlyValue>
        <ReadOnlyValue
          label={t("user.mobilePhone._")}
          hint={t("user.mobilePhone.info")}
        >
          <span className="flex flex-wrap items-baseline gap-x-3">
            <span>{data.mobilePhone || "–"}</span>
            {/* Changed there, as its hint says (it is the number of the SMS second factor). */}
            <LegacyLink url={init.my2FASetupUrl} label={t("menu.2FASetup")} />
          </span>
        </ReadOnlyValue>
        <InputField
          name="firstname"
          label={t("firstName")}
          disabled={readOnly}
        />
        <InputField name="lastname" label={t("name")} disabled={readOnly} />
      </div>
      <div className="grid gap-4 sm:grid-cols-3">
        <ReadOnlyValue label={t("login.lastLogin")}>
          {data.lastLoginFormatted}
        </ReadOnlyValue>
        <ReadOnlyValue label={t("user.changePassword.password.lastChange")}>
          {data.lastPasswordChangeFormatted}
        </ReadOnlyValue>
        {/* Only where the WLAN password can be changed at all (LDAP master), as on the legacy page. */}
        {data.wlanPasswordChangeSupported && (
          <ReadOnlyValue label={t("user.changeWlanPassword.lastChange")}>
            {data.lastWlanPasswordChangeFormatted}
          </ReadOnlyValue>
        )}
      </div>
      <ReadOnlyValue
        label={t("user.assignedGroups")}
        hint={t("user.tooltip.assignedGroups")}
      >
        {data.groups}
      </ReadOnlyValue>
    </div>
  );
}

/**
 * A value the user sees but doesn't edit here, with the label (and ⓘ) of the editable fields beside it, so
 * the card reads as one form (see FieldShell).
 */
function ReadOnlyValue({
  label,
  hint,
  children,
}: {
  label: string;
  hint?: string;
  children: ReactNode;
}) {
  const ids = useFieldIds();
  return (
    <FieldShell
      label={label}
      hint={hint}
      readOnly
      invalid={false}
      errors={[]}
      ids={ids}
      className="min-w-0"
    >
      <p
        id={ids.controlId}
        aria-labelledby={ids.labelId}
        className="min-h-7 py-1 text-sm break-words"
      >
        {children || "–"}
      </p>
    </FieldShell>
  );
}

/**
 * A page of another frontend (the password pages are still React): a full page load, as the menu does it
 * (see resolveMenuUrl).
 */
function LegacyLink({ url, label }: { url?: string | null; label: string }) {
  if (!url) return null;
  return (
    <a
      href={toAbsoluteUrl(resolveMenuUrl(url))}
      className="text-primary hover:underline"
    >
      {label}
    </a>
  );
}
