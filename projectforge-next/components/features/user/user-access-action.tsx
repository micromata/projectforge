"use client";

import { useState } from "react";
import { useMutation } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { Logout01Icon, RefreshIcon } from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import { ConfirmDialog } from "@/components/shared/confirm-dialog";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import {
  logoutAllDevicesOfUser,
  renewUserToken,
  type UserAccessInfo,
  type UserTokenType,
} from "@/lib/rs/user";
import { toast } from "@/lib/toast";

/**
 * The button of one row of UserAccessFields, behind a confirmation as both actions are irrevocable:
 * renewing the token of [tokenType], or — without one — logging out all devices of the user.
 */
export function UserAccessAction({
  userId,
  tokenType,
  onDone,
}: {
  userId: number;
  tokenType?: UserTokenType;
  onDone: (info: UserAccessInfo) => void;
}) {
  const t = useTranslations();
  const [confirmOpen, setConfirmOpen] = useState(false);
  const action = useMutation({
    mutationFn: () =>
      tokenType
        ? renewUserToken(userId, tokenType)
        : logoutAllDevicesOfUser(userId),
    onSuccess: (info) => {
      onDone(info);
      toast.success(
        tokenType
          ? t("user.authenticationToken.renew.successful")
          : t(
              "login.stayLoggedIn.invalidateAllStayLoggedInSessions.successfullDeleted"
            )
      );
    },
    onError: (error: unknown) =>
      toast.error(error instanceof Error ? error.message : String(error)),
  });
  const label = tokenType
    ? t("user.authenticationToken.renew._")
    : t("login.stayLoggedIn.invalidateAllStayLoggedInSessions._");
  return (
    <>
      <HintTooltip
        text={
          tokenType
            ? t("user.authenticationToken.renew.tooltip")
            : t("login.stayLoggedIn.invalidateAllStayLoggedInSessions.tooltip")
        }
      >
        <Button
          type="button"
          variant="outline"
          className="text-destructive"
          disabled={action.isPending}
          onClick={() => setConfirmOpen(true)}
        >
          <HugeiconsIcon
            icon={tokenType ? RefreshIcon : Logout01Icon}
            size={14}
            aria-hidden
          />
          {label}
        </Button>
      </HintTooltip>
      <ConfirmDialog
        open={confirmOpen}
        onOpenChange={setConfirmOpen}
        title={label}
        description={t("user.authenticationToken.renew.securityQuestion")}
        confirmLabel={label}
        destructive
        onConfirm={() => action.mutate()}
      />
    </>
  );
}
