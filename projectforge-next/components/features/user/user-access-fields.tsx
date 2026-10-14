"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { Button } from "@/components/ui/button";
import { useEntityData } from "@/components/shared/form/form-context";
import { ReadOnlyValue } from "@/components/shared/form/read-only-value";
import { ChangePasswordDialog } from "@/components/features/change-password/change-password-dialog";
import type { PasswordVariant } from "@/components/features/change-password/types";
import type { UserAccessInfo } from "@/lib/rs/user";
import { cn } from "@/lib/utils";
import { UserAccessAction } from "./user-access-action";
import type { UserDetail } from "./types";

/**
 * What an administrator may do about the access of an existing user without saving the form: renew one
 * of the tokens, log out every device staying logged in, and change the passwords on their pages. The
 * tokens themselves are the user's (my-account page); only when they were created shows here.
 *
 * The actions answer the refreshed dates, kept here rather than in the form — they are nothing a save
 * would write.
 */
export function UserAccessFields({ className }: { className?: string }) {
  const t = useTranslations();
  const data = useEntityData<UserDetail>();
  const [info, setInfo] = useState<UserAccessInfo>({});
  const [passwordDialog, setPasswordDialog] = useState<PasswordVariant | null>(
    null
  );
  const userId = data?.id;
  if (userId == null) return null;
  const merged = { ...data, ...info };
  const update = (next: UserAccessInfo) =>
    setInfo((old) => ({ ...old, ...next }));
  const devices = `${merged.stayLoggedInDevices ?? 0}${
    merged.stayLoggedInLastAccessTimeAgo
      ? ` (${t("login.stayLoggedIn.devices.lastAccess")}: ${merged.stayLoggedInLastAccessTimeAgo})`
      : ""
  }`;
  return (
    <div className={cn("flex flex-col gap-4", className)}>
      <div className="flex flex-wrap gap-2">
        {/* The action an admin comes here for most: first and highlighted. */}
        <Button
          type="button"
          size="sm"
          onClick={() => setPasswordDialog("password")}
        >
          {t("menu.changePassword")}
        </Button>
        {data?.wlanPasswordSupported && (
          <Button
            type="button"
            variant="outline"
            size="sm"
            onClick={() => setPasswordDialog("wlan")}
          >
            {t("menu.changeWlanPassword")}
          </Button>
        )}
      </div>
      <div className="grid items-end gap-4 sm:grid-cols-[minmax(0,1fr)_max-content]">
        <ReadOnlyValue
          label={t("user.authenticationToken.calendar_rest._")}
          hint={t("user.authenticationToken.calendar_rest.tooltip")}
        >
          {merged.calendarExportTokenCreationTimeAgo}
        </ReadOnlyValue>
        <UserAccessAction
          userId={userId}
          tokenType="CALENDAR_REST"
          onDone={update}
        />
        <ReadOnlyValue
          label={t("user.authenticationToken.dav_token._")}
          hint={t("user.authenticationToken.dav_token.tooltip")}
        >
          {merged.davTokenCreationTimeAgo}
        </ReadOnlyValue>
        <UserAccessAction
          userId={userId}
          tokenType="DAV_TOKEN"
          onDone={update}
        />
        <ReadOnlyValue
          label={t("user.authenticationToken.rest_client._")}
          hint={t("user.authenticationToken.rest_client.tooltip")}
        >
          {merged.restClientTokenCreationTimeAgo}
        </ReadOnlyValue>
        <UserAccessAction
          userId={userId}
          tokenType="REST_CLIENT"
          onDone={update}
        />
        <ReadOnlyValue
          label={t("login.stayLoggedIn.devices._")}
          hint={t("login.stayLoggedIn.devices.tooltip")}
        >
          {devices}
        </ReadOnlyValue>
        <UserAccessAction userId={userId} onDone={update} />
      </div>
      {passwordDialog && (
        <ChangePasswordDialog
          variant={passwordDialog}
          userId={userId}
          onClose={() => setPasswordDialog(null)}
        />
      )}
    </div>
  );
}
