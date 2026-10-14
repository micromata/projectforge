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
import {
  fetchCalendarSubscriptionInfo,
  type CalendarSubscriptionType,
} from "@/lib/rs/calendar-subscription";
import { CalendarSubscriptionPanel } from "./calendar-subscription-panel";

interface Props {
  type: CalendarSubscriptionType;
  /** The explanation under the heading, e.g. what subscribing the time sheets gives. */
  description: string;
  onClose: () => void;
}

/**
 * The subscription link of one of ProjectForge's calendar feeds (time sheets, holidays, weeks of year) in a
 * dialog, see [CalendarSubscriptionPanel].
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
          <CalendarSubscriptionPanel info={info} />
        )}
      </DialogContent>
    </Dialog>
  );
}
