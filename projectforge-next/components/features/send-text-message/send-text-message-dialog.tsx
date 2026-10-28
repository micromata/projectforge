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
import { fetchSendTextMessageData } from "@/lib/rs/send-text-message";
import { SendTextMessageForm } from "./send-text-message-form";
import type { SendTextMessageParams } from "./types";

/**
 * The text message in a dialog over the page it is started from — the SMS icon beside a mobile number of an
 * address in the list or the view, as the React app opened it. The page (`/next/sendTextMessage`,
 * SendTextMessagePage) is the same form, for the menu entry and direct links. Closes once the message is sent.
 */
export function SendTextMessageDialog({
  params,
  onClose,
}: {
  params: SendTextMessageParams;
  onClose: () => void;
}) {
  const t = useTranslations();
  const data = useQuery({
    queryKey: ["sendTextMessage", params],
    queryFn: ({ signal }) => fetchSendTextMessageData(params, signal),
  });
  return (
    <Dialog open onOpenChange={(open) => !open && onClose()}>
      <DialogContent className="max-h-[90vh] overflow-y-auto sm:max-w-xl">
        <DialogHeader>
          <DialogTitle>{t("address.sendSms.title")}</DialogTitle>
          <DialogDescription className="sr-only">
            {t("address.sendSms.title")}
          </DialogDescription>
        </DialogHeader>
        {data.isPending ? (
          <div className="flex justify-center py-8">
            <Spinner className="h-6 w-6 border-2" />
          </div>
        ) : data.isError ? (
          <p className="text-sm text-destructive">
            {t("access.exception.noAccess")}
          </p>
        ) : (
          <SendTextMessageForm initial={data.data} inDialog onDone={onClose} />
        )}
      </DialogContent>
    </Dialog>
  );
}
