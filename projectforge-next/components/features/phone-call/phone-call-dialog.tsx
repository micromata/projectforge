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
import { fetchPhoneCallData } from "@/lib/rs/phone-call";
import { PhoneCallForm } from "./phone-call-form";
import type { PhoneCallParams } from "./types";

/**
 * The direct call in a dialog over the page it is started from — a phone number of an address in the list or the
 * view, as the React app opened it: the list stays where it was. The page (`/next/phoneCall`, PhoneCallPage) is
 * the same form, for the menu entry and direct links. Closes once the call is placed.
 */
export function PhoneCallDialog({
  params,
  onClose,
}: {
  params: PhoneCallParams;
  onClose: () => void;
}) {
  const t = useTranslations();
  const data = useQuery({
    queryKey: ["phoneCall", params],
    queryFn: ({ signal }) => fetchPhoneCallData(params, signal),
  });
  return (
    <Dialog open onOpenChange={(open) => !open && onClose()}>
      <DialogContent className="max-h-[90vh] overflow-y-auto sm:max-w-3xl">
        <DialogHeader>
          <DialogTitle>{t("address.phoneCall.title")}</DialogTitle>
          <DialogDescription className="sr-only">
            {t("address.phoneCall.title")}
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
          <PhoneCallForm initial={data.data} inDialog onDone={onClose} />
        )}
      </DialogContent>
    </Dialog>
  );
}
