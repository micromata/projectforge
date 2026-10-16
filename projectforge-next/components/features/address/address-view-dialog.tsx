"use client";

import { useTranslations } from "next-intl";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Spinner } from "@/components/shared/spinner";
import { AddressViewActions } from "./address-view-actions";
import { AddressViewDetails } from "./address-view-details";
import { useAddressHeading } from "./use-address-heading";
import { useAddressView } from "./use-address-view";

/**
 * The view of an address in a dialog over the list — what a row opens, as the React list did: a quick look at the
 * phone numbers and the contact data that leaves the list where it was (see PageDef.rowDialog). The page of the
 * view (AddressView) shows the same and is what direct links open.
 */
export function AddressViewDialog({
  id,
  onClose,
}: {
  id: number;
  onClose: () => void;
}) {
  const t = useTranslations();
  const { query, refresh } = useAddressView(id);
  const view = query.data;
  const heading = useAddressHeading(view?.address);

  return (
    <Dialog open onOpenChange={(open) => !open && onClose()}>
      <DialogContent className="max-h-[90vh] overflow-y-auto sm:max-w-4xl">
        <DialogHeader>
          <DialogTitle>{heading || t("address.view.title")}</DialogTitle>
          {/* For screen readers; the heading says it all on screen. */}
          <DialogDescription className="sr-only">
            {t("address.view.title")}
          </DialogDescription>
        </DialogHeader>
        {query.error ? (
          <p className="text-sm text-destructive">
            {query.error instanceof Error
              ? query.error.message
              : String(query.error)}
          </p>
        ) : !view ? (
          <div className="flex justify-center py-8">
            <Spinner className="h-6 w-6 border-2" />
          </div>
        ) : (
          <>
            <div className="flex flex-wrap gap-2">
              <AddressViewActions address={view.address} inDialog />
            </div>
            <AddressViewDetails
              view={view}
              onFavoriteChanged={refresh}
              className="px-0 py-0"
            />
          </>
        )}
      </DialogContent>
    </Dialog>
  );
}
