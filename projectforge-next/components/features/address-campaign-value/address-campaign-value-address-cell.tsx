"use client";

import { useState, type ReactNode } from "react";
import { useTranslations } from "next-intl";
import { AddressViewDialog } from "@/components/features/address/address-view-dialog";
import { cn } from "@/lib/utils";

/**
 * A cell of the list showing a part of the address (the organization, the mailing address) that opens the
 * address in a dialog over the list — the React list opened the address form as a modal from these two
 * columns, so a wrong address can be corrected without losing the place in the campaign. The click stays
 * with the cell: the row's own click opens the campaign value. Its "edit" returns to this list.
 *
 * Plain text while the rows can be picked (`selecting`): a click there means "pick this row", and a button
 * swallowing it would leave the row unticked.
 */
export function AddressCampaignValueAddressCell({
  addressId,
  multiline,
  selecting,
  children,
}: {
  addressId: number | null | undefined;
  multiline?: boolean;
  selecting?: boolean;
  children: ReactNode;
}) {
  const t = useTranslations();
  const [open, setOpen] = useState(false);
  if (addressId == null || selecting)
    return multiline ? (
      <span className="whitespace-pre-line">{children}</span>
    ) : (
      <>{children}</>
    );
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
        // The keys that press the button are the button's: the table's keyboard handlers sit on its body
        // and would otherwise act on the focused row as well (see useRowSelection).
        onKeyDown={(event) => {
          if (event.key === " " || event.key === "Enter")
            event.stopPropagation();
        }}
      >
        {empty ? t("emptyInBrackets") : children}
      </button>
      {/* The dialog is portaled out of the table, but React still bubbles its events through the cell: a click on
          its links or buttons would reach the row and open the campaign value instead. */}
      {open && (
        <span
          className="contents"
          onClick={(event) => event.stopPropagation()}
          onKeyDown={(event) => event.stopPropagation()}
        >
          <AddressViewDialog
            id={addressId}
            onClose={() => setOpen(false)}
            returnTo="/address-campaign-value"
          />
        </span>
      )}
    </>
  );
}
