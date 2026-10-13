"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { useQueryClient } from "@tanstack/react-query";
import { HugeiconsIcon } from "@hugeicons/react";
import { Logout01Icon } from "@hugeicons/core-free-icons";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { ConfirmDialog } from "@/components/shared/confirm-dialog";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import { RsError } from "@/lib/rs/client";
import {
  logoutAllMyAccountDevices,
  logoutMyAccountDevice,
  MY_ACCOUNT_QUERY_KEY,
} from "@/lib/rs/my-account";
import { toast } from "@/lib/toast";
import type { MyAccountDevice, MyAccountInit } from "./types";

/** What is to be confirmed: one device (by id) or all of them. */
type Pending = { kind: "one"; id: number } | { kind: "all" };

/**
 * The devices staying logged in (one stay-logged-in token each), most recently used first, with the one
 * this page was loaded on marked. Each can be logged out on its own, or all at once. Plain rows rather
 * than a DataTable: a user has a handful of devices, nothing to sort, filter or page.
 */
export function MyAccountDevices({ devices }: { devices: MyAccountDevice[] }) {
  const t = useTranslations();
  const queryClient = useQueryClient();
  const [pending, setPending] = useState<Pending | null>(null);
  const [busy, setBusy] = useState(false);

  const logout = async (target: Pending) => {
    setBusy(true);
    try {
      const remaining =
        target.kind === "all"
          ? await logoutAllMyAccountDevices()
          : await logoutMyAccountDevice(target.id);
      queryClient.setQueryData<MyAccountInit>(MY_ACCOUNT_QUERY_KEY, (old) =>
        old ? { ...old, data: { ...old.data, devices: remaining } } : old
      );
      toast.success(
        target.kind === "all"
          ? t(
              "login.stayLoggedIn.invalidateAllStayLoggedInSessions.successfullDeleted"
            )
          : t("login.stayLoggedIn.devices.logout.successful")
      );
    } catch (error) {
      toast.error(error instanceof RsError ? error.message : String(error));
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="flex flex-col gap-3">
      <p className="text-sm text-muted-foreground">
        {t("login.stayLoggedIn.devices.tooltip")}
      </p>
      {devices.length === 0 ? (
        <p className="text-sm">{t("login.stayLoggedIn.devices.none")}</p>
      ) : (
        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm">
            <thead className="text-xs text-muted-foreground">
              <tr>
                <th className="py-1 pr-3 font-medium">
                  {t("login.stayLoggedIn.devices.userAgent")}
                </th>
                <th className="py-1 pr-3 font-medium">
                  {t("login.stayLoggedIn.devices.ip")}
                </th>
                <th className="py-1 pr-3 font-medium">
                  {t("login.stayLoggedIn.devices.lastAccess")}
                </th>
                <th className="py-1 pr-3 font-medium">
                  {t("login.stayLoggedIn.devices.created")}
                </th>
                <th className="py-1" />
              </tr>
            </thead>
            <tbody>
              {devices.map((device) => (
                <tr
                  key={device.id}
                  data-testid="my-account-device"
                  className="border-t border-border align-top"
                >
                  <td className="py-1.5 pr-3 break-all">
                    {device.userAgent}
                    {device.current && (
                      <Badge variant="secondary" className="ml-2">
                        {t("login.stayLoggedIn.devices.current")}
                      </Badge>
                    )}
                  </td>
                  <td className="py-1.5 pr-3 whitespace-nowrap">
                    {device.lastAccessIp}
                  </td>
                  <td className="py-1.5 pr-3 whitespace-nowrap">
                    {device.lastAccessFormatted}
                  </td>
                  <td className="py-1.5 pr-3 whitespace-nowrap">
                    {device.createdFormatted}
                  </td>
                  <td className="py-1 text-right">
                    <Button
                      type="button"
                      variant="outline"
                      className="text-destructive"
                      disabled={busy}
                      onClick={() => setPending({ kind: "one", id: device.id })}
                    >
                      <HugeiconsIcon
                        icon={Logout01Icon}
                        size={14}
                        aria-hidden
                      />
                      {t("login.stayLoggedIn.devices.logout._")}
                    </Button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
      {devices.length > 0 && (
        <div>
          <HintTooltip
            text={t(
              "login.stayLoggedIn.invalidateAllStayLoggedInSessions.tooltip"
            )}
          >
            <Button
              type="button"
              variant="outline"
              className="text-destructive"
              disabled={busy}
              onClick={() => setPending({ kind: "all" })}
            >
              <HugeiconsIcon icon={Logout01Icon} size={14} aria-hidden />
              {t("login.stayLoggedIn.invalidateAllStayLoggedInSessions._")}
            </Button>
          </HintTooltip>
        </div>
      )}
      <ConfirmDialog
        open={pending != null}
        onOpenChange={(open) => !open && setPending(null)}
        title={
          pending?.kind === "all"
            ? t("login.stayLoggedIn.invalidateAllStayLoggedInSessions._")
            : t("login.stayLoggedIn.devices.logout._")
        }
        description={
          pending?.kind === "all"
            ? t("login.stayLoggedIn.invalidateAllStayLoggedInSessions.question")
            : t("login.stayLoggedIn.devices.logout.question")
        }
        confirmLabel={t("login.stayLoggedIn.devices.logout._")}
        destructive
        onConfirm={() => {
          if (pending) void logout(pending);
          setPending(null);
        }}
      />
    </div>
  );
}
