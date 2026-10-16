"use client";

import Link from "next/link";
import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { PencilEdit02Icon } from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import { ADDRESS_ROUTE, addressEditRoute } from "./address-routes";

/**
 * The pencil of a list row: opens the form of the address straight away, as the React list's edit column did,
 * while a click on the row opens its view. The form returns to the list. Shown for every row — the list does not
 * know the write access per address; the form opens read only where it is missing.
 */
export function AddressEditLink({ addressId }: { addressId: number }) {
  const t = useTranslations();
  return (
    <Button
      asChild
      variant="ghost"
      size="icon"
      className="size-7 text-muted-foreground"
    >
      <Link
        href={`${addressEditRoute(addressId)}?returnTo=${ADDRESS_ROUTE}`}
        aria-label={t("edit")}
        // In a list row: the click opens the form, not the dialog of the view.
        onClick={(event) => event.stopPropagation()}
      >
        <HugeiconsIcon icon={PencilEdit02Icon} size={16} />
      </Link>
    </Button>
  );
}
