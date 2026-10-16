"use client";

import Link from "next/link";
import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import {
  ArrowLeft01Icon,
  Download04Icon,
  PencilEdit02Icon,
  PrinterIcon,
} from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import { useExportDownload } from "@/hooks/use-export-download";
import { leafKeyOf } from "@/lib/leaf-key";
import { downloadAddressVCard } from "@/lib/rs/address";
import {
  ADDRESS_ROUTE,
  addressEditRoute,
  addressViewRoute,
} from "./address-routes";
import type { AddressDetail } from "./types";

interface Props {
  address: AddressDetail;
  /** Where "back" leads: the page that opened the view, else the list. None in the dialog of the list. */
  back?: { route: string; labelKey: string };
  /**
   * Shown in the dialog over the list: printing leads to the page of the address (a dialog prints badly), and the
   * form returns to the list rather than to the page of the view.
   */
  inDialog?: boolean;
}

/**
 * The buttons of the address view: back, the address as vCard, print, and the form — the last only for a user who
 * may change the address (`writeAccess`).
 */
export function AddressViewActions({ address, back, inDialog }: Props) {
  const t = useTranslations();
  const vCard = useExportDownload(() => downloadAddressVCard(address.id!));
  return (
    <>
      {back && (
        <Button asChild variant="outline">
          <Link href={back.route}>
            <HugeiconsIcon icon={ArrowLeft01Icon} />
            {t(leafKeyOf(back.labelKey, t.has))}
          </Link>
        </Button>
      )}
      <Button
        type="button"
        variant="outline"
        disabled={vCard.isPending}
        onClick={() => vCard.mutate()}
      >
        <HugeiconsIcon icon={Download04Icon} />
        {t("address.book.vCardSingleExport")}
      </Button>
      {inDialog && address.id != null ? (
        <Button asChild variant="outline">
          <Link href={addressViewRoute(address.id)}>
            <HugeiconsIcon icon={PrinterIcon} />
            {t("printView")}
          </Link>
        </Button>
      ) : (
        <Button type="button" variant="outline" onClick={() => window.print()}>
          <HugeiconsIcon icon={PrinterIcon} />
          {t("print")}
        </Button>
      )}
      {address.writeAccess !== false && address.id != null && (
        <Button asChild>
          <Link
            href={
              inDialog
                ? `${addressEditRoute(address.id)}?returnTo=${ADDRESS_ROUTE}`
                : addressEditRoute(address.id)
            }
          >
            <HugeiconsIcon icon={PencilEdit02Icon} />
            {t("edit")}
          </Link>
        </Button>
      )}
    </>
  );
}
