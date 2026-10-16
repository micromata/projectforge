"use client";

import { leafKeyOf } from "@/lib/leaf-key";
import { useQuery } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { CopyableValue } from "@/components/shared/copyable-value";
import { Spinner } from "@/components/shared/spinner";
import { CARD_DAV_INFO_QUERY_KEY, fetchCardDavInfo } from "@/lib/rs/address";

/**
 * How to subscribe the favorite addresses in an address book app via CardDAV: user, token and the server urls
 * for the clients that want them in different shapes (`CardDAVInfoRest`). The token is masked until revealed.
 *
 * The client names are product names and stay as they are in every language.
 */
export function CardDavInfoDialog({ onClose }: { onClose: () => void }) {
  const t = useTranslations();
  const query = useQuery({
    queryKey: CARD_DAV_INFO_QUERY_KEY,
    queryFn: ({ signal }) => fetchCardDavInfo(signal),
  });
  const info = query.data;
  const rows: { label: string; value?: string; masked?: boolean }[] = [
    { label: t(leafKeyOf("user", t.has)), value: info?.user },
    {
      label: t(leafKeyOf("password", t.has)),
      value: info?.password,
      masked: true,
    },
    { label: "Server (Apple macOS)", value: info?.appleUrl },
    { label: "Path (Apple macOS)", value: info?.applePath },
    { label: "Server (Thunderbird etc.)", value: info?.standardUrl },
    { label: "Url (Apple iOS)", value: info?.iOSUrl },
  ];

  return (
    <Dialog open onOpenChange={(open) => !open && onClose()}>
      <DialogContent className="sm:max-w-xl">
        <DialogHeader>
          <DialogTitle>{t("address.cardDAV.infopage.title")}</DialogTitle>
          <DialogDescription>
            {t("address.cardDAV.infopage.description")}
          </DialogDescription>
        </DialogHeader>
        <p className="text-sm text-muted-foreground">
          {t("address.cardDAV.infopage.description2")}{" "}
          {t("address.cardDAV.infopage.description3")}
        </p>
        {query.isLoading ? (
          <div className="flex justify-center py-4">
            <Spinner className="h-5 w-5 border-2" />
          </div>
        ) : (
          <dl className="grid gap-2 sm:grid-cols-[auto_1fr] sm:items-center sm:gap-x-3">
            {rows.map((row) => (
              <div key={row.label} className="contents">
                <dt className="text-sm text-muted-foreground">{row.label}</dt>
                <dd>
                  <CopyableValue
                    value={row.value ?? ""}
                    label={row.label}
                    masked={row.masked}
                    peek={row.masked}
                  />
                </dd>
              </div>
            ))}
          </dl>
        )}
      </DialogContent>
    </Dialog>
  );
}
