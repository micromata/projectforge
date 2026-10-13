"use client";

import { useTranslations } from "next-intl";
import { useQuery } from "@tanstack/react-query";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Spinner } from "@/components/shared/spinner";
import { fetchMyAccountTokenInfo } from "@/lib/rs/my-account";
import type { MyAccountTokenType } from "./types";

interface Props {
  type: MyAccountTokenType;
  /** The token's name, e.g. "DAV token". */
  label: string;
  onClose: () => void;
}

/**
 * The usage of an access token: the clients that used it since the server was started (user agent, IP,
 * last access, counter), for spotting a misuse. For the REST client token also the QR code for setting
 * up a client app. The successor of the legacy TokenInfoPageRest modal.
 */
export function TokenInfoDialog({ type, label, onClose }: Props) {
  const t = useTranslations();
  const query = useQuery({
    queryKey: ["myAccount", "tokenInfo", type],
    queryFn: ({ signal }) => fetchMyAccountTokenInfo(type, signal),
    // The QR code carries a short-lived link, and the accesses change with every use.
    gcTime: 0,
  });
  const info = query.data;
  const accesses = info?.accesses ?? [];

  return (
    <Dialog open onOpenChange={(open) => !open && onClose()}>
      <DialogContent className="sm:max-w-2xl">
        <DialogHeader>
          <DialogTitle>{label}</DialogTitle>
          <DialogDescription>
            {t("user.authenticationToken.button.showUsage.tooltip")}
          </DialogDescription>
        </DialogHeader>
        {query.isPending ? (
          <div className="flex justify-center py-4">
            <Spinner className="h-5 w-5 border-2" />
          </div>
        ) : query.isError ? (
          <p className="text-sm text-destructive">
            {t("access.exception.noAccess")}
          </p>
        ) : (
          <>
            {accesses.length === 0 ? (
              <p className="text-sm text-muted-foreground">
                {t("user.authenticationToken.usage.none")}
              </p>
            ) : (
              <div className="max-h-80 overflow-auto">
                <table className="w-full text-left text-xs">
                  <thead className="text-muted-foreground">
                    <tr>
                      <th className="py-1 pr-2 font-medium">
                        {t("login.stayLoggedIn.devices.userAgent")}
                      </th>
                      <th className="py-1 pr-2 font-medium">
                        {t("login.stayLoggedIn.devices.ip")}
                      </th>
                      <th className="py-1 pr-2 font-medium">
                        {t("login.stayLoggedIn.devices.lastAccess")}
                      </th>
                      <th className="py-1 text-right font-medium">
                        {t("user.authenticationToken.usage.counter")}
                      </th>
                    </tr>
                  </thead>
                  <tbody>
                    {accesses.map((access, index) => (
                      <tr key={index} className="border-t border-border">
                        <td className="py-1 pr-2 break-all">
                          {access.userAgent}
                        </td>
                        <td className="py-1 pr-2 whitespace-nowrap">
                          {access.ip}
                        </td>
                        <td className="py-1 pr-2 whitespace-nowrap">
                          {access.lastAccessFormatted}
                        </td>
                        <td className="py-1 text-right">{access.counter}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
            {info?.qrCodeUrl && (
              <figure className="flex flex-col items-center gap-2">
                {/* eslint-disable-next-line @next/next/no-img-element -- rendered by the backend at runtime. */}
                <img
                  src={info.qrCodeUrl}
                  alt={t("user.authenticationToken.usage.qrCode")}
                  className="size-48"
                />
                <figcaption className="text-xs text-muted-foreground">
                  {t("user.authenticationToken.usage.qrCode")}
                </figcaption>
              </figure>
            )}
          </>
        )}
      </DialogContent>
    </Dialog>
  );
}
