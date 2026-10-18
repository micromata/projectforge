"use client";

import { useState, type ReactNode } from "react";
import { useTranslations } from "next-intl";
import { AddressViewDialog } from "@/components/features/address/address-view-dialog";
import { cn } from "@/lib/utils";

/**
 * A cell of the list showing a part of the address (the organization, the mailing address) that opens the
 * address in a dialog over the list — the React list opened the address form as a modal from these two
 * columns, so a wrong address can be corrected without losing the place in the campaign. The click stays
 * with the cell: the row's own click opens the campaign value.
 */
export function AddressCampaignValueAddressCell({
  addressId,
  multiline,
  children,
}: {
  addressId: number | null | undefined;
  multiline?: boolean;
  children: ReactNode;
}) {
  const t = useTranslations();
  const [open, setOpen] = useState(false);
  if (addressId == null) return <>{children}</>;
  const empty = children == null || children === "";
  return (
    <>
      <button
        type="button"
        className={cn(
          "text-left text-primary underline-offset-2 hover:underline",
          multiline && "whitespace-pre-line",
          empty && "text-muted-foreground"
        )}
        onClick={(event) => {
          event.stopPropagation();
          setOpen(true);
        }}
      >
        {empty ? t("emptyInBrackets") : children}
      </button>
      {open && (
        <AddressViewDialog id={addressId} onClose={() => setOpen(false)} />
      )}
    </>
  );
}
