"use client";

import { useTranslations } from "next-intl";
import { useQuery } from "@tanstack/react-query";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { CopyableValue } from "@/components/shared/copyable-value";
import { Spinner } from "@/components/shared/spinner";
import {
  fetchCalendarSubscriptionInfo,
  type CalendarSubscriptionType,
} from "@/lib/rs/calendar-subscription";

interface Props {
  type: CalendarSubscriptionType;
  /** The explanation under the heading, e.g. what subscribing the time sheets gives. */
  description: string;
  onClose: () => void;
}

/**
 * The subscription link of one of ProjectForge's calendar feeds (time sheets, holidays, weeks of year) for
 * the user to add to their calendar app: the url to copy, a QR code for a phone, and the security advice —
 * the url carries the user's personal login token (as the legacy `AbstractICSExportDialog`).
 */
export function CalendarSubscriptionDialog({
  type,
  description,
  onClose,
}: Props) {
  const t = useTranslations();
  const query = useQuery({
    queryKey: ["calendarSubscription", type],
    queryFn: ({ signal }) => fetchCalendarSubscriptionInfo(type, signal),
  });
  const info = query.data;
  const url = info?.url;

  return (
    <Dialog open onOpenChange={(open) => !open && onClose()}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>
            {info?.headline ?? t("plugins.teamcal.subscription._")}
          </DialogTitle>
          <DialogDescription>{description}</DialogDescription>
        </DialogHeader>
        {query.isLoading ? (
          <div className="flex justify-center py-4">
            <Spinner className="h-5 w-5 border-2" />
          </div>
        ) : (
          <>
            <CopyableValue
              value={url}
              label={info?.headline ?? t("plugins.teamcal.subscription._")}
            />
            {url && info?.barcodeUrl && (
              // eslint-disable-next-line @next/next/no-img-element -- rendered by the backend at runtime.
              <img
                src={`${info.barcodeUrl}?text=${encodeURIComponent(url)}`}
                alt={info.headline ?? ""}
                className="size-48 self-center"
              />
            )}
          </>
        )}
        {info?.securityAdvise && (
          <Alert variant="destructive">
            <AlertTitle>{info.securityAdviseHeadline}</AlertTitle>
            <AlertDescription>{info.securityAdvise}</AlertDescription>
          </Alert>
        )}
      </DialogContent>
    </Dialog>
  );
}
