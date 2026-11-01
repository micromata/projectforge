"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { useQueryClient } from "@tanstack/react-query";
import { HugeiconsIcon } from "@hugeicons/react";
import { Logout01Icon } from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import { ConfirmDialog } from "@/components/shared/confirm-dialog";
import { RsError } from "@/lib/rs/client";
import {
  logoutMyAccountAppDevice,
  MY_ACCOUNT_QUERY_KEY,
} from "@/lib/rs/my-account";
import { toast } from "@/lib/toast";
import type { MyAccountAppDevice, MyAccountInit } from "./types";

/**
 * The connected ProjectForge apps (one app token each, AppDeviceDO), most recently used first. Each can be
 * logged out: its token is deleted and the app asks for a new login in the browser. Plain rows like
 * [MyAccountDevices]: a user has a handful of apps.
 */
export function MyAccountAppDevices({
  devices,
  canLogout,
}: {
  devices: MyAccountAppDevice[];
  /** False for a restricted or demo user (a shared account). */
  canLogout: boolean;
}) {
  const t = useTranslations();
  const queryClient = useQueryClient();
  const [pendingId, setPendingId] = useState<number | null>(null);
  const [busy, setBusy] = useState(false);

  const logout = async (id: number) => {
    setBusy(true);
    try {
      const remaining = await logoutMyAccountAppDevice(id);
      queryClient.setQueryData<MyAccountInit>(MY_ACCOUNT_QUERY_KEY, (old) =>
        old ? { ...old, data: { ...old.data, appDevices: remaining } } : old
      );
      toast.success(t("mobileApp.devices.logout.successful"));
    } catch (error) {
      toast.error(error instanceof RsError ? error.message : String(error));
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="flex flex-col gap-3">
      <p className="text-sm text-muted-foreground">
        {t("mobileApp.devices.tooltip")}
      </p>
      {devices.length === 0 ? (
        <p className="text-sm">{t("mobileApp.devices.none")}</p>
      ) : (
        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm">
            <thead className="text-xs text-muted-foreground">
              <tr>
                <th className="py-1 pr-3 font-medium">
                  {t("mobileApp.devices.device")}
                </th>
                <th className="py-1 pr-3 font-medium">
                  {t("mobileApp.devices.platform")}
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
                  data-testid="my-account-app-device"
                  className="border-t border-border align-top"
                >
                  <td className="py-1.5 pr-3 break-all">{device.deviceName}</td>
                  <td className="py-1.5 pr-3 whitespace-nowrap">
                    {[device.platform, device.appVersion]
                      .filter(Boolean)
                      .join(" ")}
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
                    {canLogout && (
                      <Button
                        type="button"
                        variant="outline"
                        className="text-destructive"
                        disabled={busy}
                        onClick={() => setPendingId(device.id)}
                      >
                        <HugeiconsIcon
                          icon={Logout01Icon}
                          size={14}
                          aria-hidden
                        />
                        {t("login.stayLoggedIn.devices.logout._")}
                      </Button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
      <ConfirmDialog
        open={pendingId != null}
        onOpenChange={(open) => !open && setPendingId(null)}
        title={t("login.stayLoggedIn.devices.logout._")}
        description={t("mobileApp.devices.logout.question")}
        confirmLabel={t("login.stayLoggedIn.devices.logout._")}
        destructive
        onConfirm={() => {
          if (pendingId != null) void logout(pendingId);
          setPendingId(null);
        }}
      />
    </div>
  );
}
